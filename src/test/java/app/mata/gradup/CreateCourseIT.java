package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.endpoint.rest.model.CourseCreateRequest;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JTrack;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public class CreateCourseIT extends SecuredFacadeIT {

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
  void createCourse_returnsCreatedCourse() {
    var track = saveTrack("EL", "Electronique");

    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("Pro1")
                .title("Programmation")
                .credits(4)
                .semesterNumber(1)
                .trackId(track.getId()),
            CourseResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertNotNull(course.getId());
    assertEquals("Pro1", course.getReference());
    assertEquals("Programmation", course.getTitle());
    assertEquals(4, course.getCredits());
    assertEquals(1, course.getSemesterNumber());
    assertNotNull(course.getTrack());
    assertEquals(track.getId(), course.getTrack().getId());
    assertEquals("EL", course.getTrack().getCode().toString());
    assertTrue(courseRepository.existsById(course.getId()));
    var saved = courseRepository.findById(course.getId()).orElseThrow();
    assertEquals("Pro1", saved.getReference());
    assertEquals(track.getId(), saved.getTrack().getId());
  }

  @Test
  void createCourse_commonCourse_hasNullTrack() {
    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("Math1")
                .title("Mathematiques")
                .credits(5)
                .semesterNumber(2),
            CourseResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertNull(course.getTrack());
  }

  @Test
  void createCourse_duplicateReference_returnsConflict() {
    var track = saveTrack("EL", "Electronique");
    saveCourse("Pro1", "Programmation", 4, 1, track);

    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("Pro1")
                .title("Programmation avancee")
                .credits(4)
                .semesterNumber(1),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void createCourse_unknownTrack_returnsNotFound() {
    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("Pro1")
                .title("Programmation")
                .credits(4)
                .semesterNumber(1)
                .trackId(UUID.randomUUID()),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void createCourse_blankReference_returnsBadRequest() {
    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference(" ")
                .title("Programmation")
                .credits(4)
                .semesterNumber(1),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void createCourse_invalidCredits_returnsBadRequest() {
    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("Pro1")
                .title("Programmation")
                .credits(0)
                .semesterNumber(1),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void createCourse_invalidSemesterNumber_returnsBadRequest() {
    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("Pro1")
                .title("Programmation")
                .credits(4)
                .semesterNumber(7),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  private JTrack saveTrack(String code, String label) {
    return trackRepository.save(
        JTrack.builder().code(app.mata.gradup.model.TrackCode.valueOf(code)).label(label).build());
  }

  private void saveCourse(
      String reference, String title, int credits, int semesterNumber, JTrack track) {
    courseRepository.save(
        app.mata.gradup.repository.model.JCourse.builder()
            .reference(reference)
            .title(title)
            .credits(credits)
            .semesterNumber(semesterNumber)
            .track(track)
            .build());
  }
}
