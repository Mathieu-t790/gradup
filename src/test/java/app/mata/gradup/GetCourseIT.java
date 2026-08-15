package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.mata.gradup.conf.CourseFacadeIT;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class GetCourseIT extends CourseFacadeIT {

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
}
