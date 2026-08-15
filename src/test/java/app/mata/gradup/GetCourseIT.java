package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
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
import org.springframework.http.HttpStatus;

public class GetCourseIT extends SecuredFacadeIT {

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
  void getCourse_returnsCourseWithTrack() {
    var track = saveTrack("EL", "Electronique");
    var saved = saveCourse("Pro1", "Programmation", 4, 1, track);

    var response = restTemplate.getForEntity("/courses/" + saved.getId(), CourseResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertEquals(saved.getId(), course.getId());
    assertEquals("Pro1", course.getReference());
    assertEquals("Programmation", course.getTitle());
    assertEquals(4, course.getCredits());
    assertEquals(1, course.getSemesterNumber());
    assertNotNull(course.getTrack());
    assertEquals(track.getId(), course.getTrack().getId());
    assertEquals("EL", course.getTrack().getCode().toString());
  }

  @Test
  void getCourse_commonCourse_hasNullTrack() {
    var saved = saveCourse("Math1", "Mathematiques", 5, 2, null);

    var response = restTemplate.getForEntity("/courses/" + saved.getId(), CourseResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertEquals("Math1", course.getReference());
    assertNull(course.getTrack());
  }

  @Test
  void getCourse_unknownCourse_returnsNotFound() {
    var response = restTemplate.getForEntity("/courses/" + UUID.randomUUID(), Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
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
