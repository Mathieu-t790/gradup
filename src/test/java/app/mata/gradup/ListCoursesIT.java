package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JTrack;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public class ListCoursesIT extends SecuredFacadeIT {

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
  void listCourses_emptyCatalog_returnsEmptyList() {
    var response = restTemplate.getForEntity("/courses", CourseResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().length);
  }

  @Test
  void listCourses_returnsAllCourses() {
    var trackEl = saveTrack("EL", "Electronique");
    saveCourse("Pro1", "Programmation", 4, 1, trackEl);
    saveCourse("Web1", "Web", 3, 1, null);
    saveCourse("Math1", "Mathematiques", 5, 2, null);

    var response = restTemplate.getForEntity("/courses", CourseResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var courses = List.of(response.getBody());
    assertEquals(3, courses.size());
    assertEquals(
        List.of("Math1", "Pro1", "Web1"),
        courses.stream().map(CourseResponse::getReference).sorted().toList());
  }

  @Test
  void listCourses_filterByTrack_returnsTrackAndCommonCourses() {
    var trackEl = saveTrack("EL", "Electronique");
    saveCourse("Pro1", "Programmation", 4, 1, trackEl);
    saveCourse("Web1", "Web", 3, 1, null);
    saveCourse("Math1", "Mathematiques", 5, 2, null);

    var response =
        restTemplate.getForEntity("/courses?trackId=" + trackEl.getId(), CourseResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var courses = List.of(response.getBody());
    assertEquals(2, courses.size());
    assertEquals(
        List.of("Pro1", "Web1"),
        courses.stream().map(CourseResponse::getReference).sorted().toList());
  }

  @Test
  void listCourses_filterBySemesterNumber_returnsMatchingCourses() {
    var trackEl = saveTrack("EL", "Electronique");
    saveCourse("Pro1", "Programmation", 4, 1, trackEl);
    saveCourse("Math1", "Mathematiques", 5, 2, null);

    var response = restTemplate.getForEntity("/courses?semesterNumber=2", CourseResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var courses = List.of(response.getBody());
    assertEquals(1, courses.size());
    assertEquals("Math1", courses.get(0).getReference());
  }

  @Test
  void listCourses_filterByTrackAndSemester_returnsMatchingCourses() {
    var trackEl = saveTrack("EL", "Electronique");
    var trackTn = saveTrack("TN", "Telecom");
    saveCourse("Pro1", "Programmation", 4, 1, trackEl);
    saveCourse("Math1", "Mathematiques", 5, 2, null);
    saveCourse("Math1tn", "Mathematiques", 5, 2, trackTn);

    var response =
        restTemplate.getForEntity(
            "/courses?trackId=" + trackTn.getId() + "&semesterNumber=2", CourseResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var courses = List.of(response.getBody());
    assertEquals(2, courses.size());
    assertTrue(courses.stream().anyMatch(c -> c.getReference().equals("Math1")));
    assertTrue(courses.stream().anyMatch(c -> c.getReference().equals("Math1tn")));
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
}
