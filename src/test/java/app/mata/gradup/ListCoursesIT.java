package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.CourseFacadeIT;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class ListCoursesIT extends CourseFacadeIT {

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
    var trackTn = saveTrack("TN", "Telecom");
    saveCourse("Pro1", "Programmation", 4, 1, trackEl);
    saveCourse("Web1", "Web", 3, 1, null);
    saveCourse("Math1", "Mathematiques", 5, 2, trackTn);

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
}
