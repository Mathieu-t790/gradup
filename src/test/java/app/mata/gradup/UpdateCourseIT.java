package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
import app.mata.gradup.endpoint.rest.model.CourseUpdateRequest;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JTrack;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class UpdateCourseIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private CourseRepository courseRepository;
  @Autowired private TrackRepository trackRepository;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  private void cleanDatabase() {
    courseRepository.deleteAll();
    trackRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void updateCourse_updatesTitleAndCredits() {
    var track = saveTrack("EL", "Electronique");
    var saved = saveCourse("Pro1", "Programmation", 4, 1, track);

    var response =
        patch(
            "/courses/" + saved.getId(),
            new CourseUpdateRequest().title("Programmation avancee").credits(5),
            CourseResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertEquals("Pro1", course.getReference());
    assertEquals("Programmation avancee", course.getTitle());
    assertEquals(5, course.getCredits());
    assertEquals(1, course.getSemesterNumber());
    assertEquals(track.getId(), course.getTrack().getId());
    var reloaded = courseRepository.findById(saved.getId()).orElseThrow();
    assertEquals("Programmation avancee", reloaded.getTitle());
    assertEquals(5, reloaded.getCredits());
  }

  @Test
  void updateCourse_changesTrack() {
    var trackEl = saveTrack("EL", "Electronique");
    var trackTn = saveTrack("TN", "Telecom");
    var saved = saveCourse("Pro1", "Programmation", 4, 1, trackEl);

    var response =
        patch(
            "/courses/" + saved.getId(),
            new CourseUpdateRequest().trackId(trackTn.getId()),
            CourseResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertNotNull(course.getTrack());
    assertEquals(trackTn.getId(), course.getTrack().getId());
    assertEquals("TN", course.getTrack().getCode().toString());
  }

  @Test
  void updateCourse_explicitNullTrack_clearsTrack() {
    var track = saveTrack("EL", "Electronique");
    var saved = saveCourse("Pro1", "Programmation", 4, 1, track);

    var response =
        patch(
            "/courses/" + saved.getId(),
            new CourseUpdateRequest().trackId(null),
            CourseResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertNull(course.getTrack());
    var reloaded = courseRepository.findById(saved.getId()).orElseThrow();
    assertNull(reloaded.getTrack());
  }

  @Test
  void updateCourse_unknownTrack_returnsNotFound() {
    var track = saveTrack("EL", "Electronique");
    var saved = saveCourse("Pro1", "Programmation", 4, 1, track);

    var response =
        patch(
            "/courses/" + saved.getId(),
            new CourseUpdateRequest().trackId(UUID.randomUUID()),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void updateCourse_unknownCourse_returnsNotFound() {
    var response =
        patch(
            "/courses/" + UUID.randomUUID(),
            new CourseUpdateRequest().title("Programmation avancee"),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void updateCourse_invalidCredits_returnsBadRequest() {
    var saved = saveCourse("Pro1", "Programmation", 4, 1, null);

    var response =
        patch("/courses/" + saved.getId(), new CourseUpdateRequest().credits(0), Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  private JTrack saveTrack(String code, String label) {
    return trackRepository.save(
        JTrack.builder().code(app.mata.gradup.model.TrackCode.valueOf(code)).label(label).build());
  }

  private JCourse saveCourse(
      String reference, String title, int credits, int semesterNumber, JTrack track) {
    return courseRepository.save(
        JCourse.builder()
            .reference(reference)
            .title(title)
            .credits(credits)
            .semesterNumber(semesterNumber)
            .track(track)
            .build());
  }

  private <T> ResponseEntity<T> patch(String url, Object body, Class<T> responseType) {
    return restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body), responseType);
  }
}
