package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.CourseCreateRequest;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
import app.mata.gradup.endpoint.rest.model.CourseUpdateRequest;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.CourseRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CourseIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private CourseRepository courseRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void createCourse_returnsCreatedCourse() {
    var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");

    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("PROG1")
                .title("Algorithmique")
                .credits(6)
                .semesterNumber(1)
                .trackId(track.getId()),
            CourseResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertNotNull(course.getId());
    assertEquals("PROG1", course.getReference());
    assertEquals("Algorithmique", course.getTitle());
    assertEquals(6, course.getCredits());
    assertEquals(1, course.getSemesterNumber());
    assertNotNull(course.getTrack());
    assertEquals(track.getId(), course.getTrack().getId());
    assertEquals("EL", course.getTrack().getCode().toString());
    assertTrue(courseRepository.existsById(course.getId()));
    var saved = courseRepository.findById(course.getId()).orElseThrow();
    assertEquals("PROG1", saved.getReference());
    assertEquals(track.getId(), saved.getTrack().getId());
  }

  @Test
  void createCourse_commonCourse_hasNullTrack() {
    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("WEB1")
                .title("Interfaces web")
                .credits(6)
                .semesterNumber(1),
            CourseResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertNull(course.getTrack());
  }

  @Test
  void createCourse_duplicateReference_returnsConflict() {
    var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    seeder.course("PROG1", "Algorithmique", 6, 1, track);

    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("PROG1")
                .title("Algorithmique avancee")
                .credits(6)
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
                .reference("PROG1")
                .title("Algorithmique")
                .credits(6)
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
                .title("Algorithmique")
                .credits(6)
                .semesterNumber(1),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void createCourse_invalidCredits_returnsBadRequest() {
    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("PROG1")
                .title("Algorithmique")
                .credits(0)
                .semesterNumber(1),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void createCourse_invalidSemesterNumber_returnsBadRequest() {
    var response =
        restTemplate.postForEntity(
            "/courses",
            new CourseCreateRequest()
                .reference("PROG1")
                .title("Algorithmique")
                .credits(6)
                .semesterNumber(7),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void getCourse_returnsCourseWithTrack() {
    var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var saved = seeder.course("PROG1", "Algorithmique", 6, 1, track);

    var response = restTemplate.getForEntity("/courses/" + saved.getId(), CourseResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertEquals(saved.getId(), course.getId());
    assertEquals("PROG1", course.getReference());
    assertEquals("Algorithmique", course.getTitle());
    assertEquals(6, course.getCredits());
    assertEquals(1, course.getSemesterNumber());
    assertNotNull(course.getTrack());
    assertEquals(track.getId(), course.getTrack().getId());
    assertEquals("EL", course.getTrack().getCode().toString());
  }

  @Test
  void getCourse_commonCourse_hasNullTrack() {
    var saved = seeder.course("THEORIE1", "Mathematiques de l'informatique", 4, 2, null);

    var response = restTemplate.getForEntity("/courses/" + saved.getId(), CourseResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertEquals("THEORIE1", course.getReference());
    assertNull(course.getTrack());
  }

  @Test
  void getCourse_unknownCourse_returnsNotFound() {
    var response = restTemplate.getForEntity("/courses/" + UUID.randomUUID(), Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
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
    var trackEl = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var trackTn = seeder.track(TrackCode.TN, "Transformation Numerique");
    seeder.course("PROG1", "Algorithmique", 6, 1, trackEl);
    seeder.course("WEB1", "Interfaces web", 6, 1, null);
    seeder.course("TN1", "Marketing Digital", 3, 4, trackTn);

    var response = restTemplate.getForEntity("/courses", CourseResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var courses = List.of(response.getBody());
    assertEquals(3, courses.size());
    assertEquals(
        List.of("PROG1", "TN1", "WEB1"),
        courses.stream().map(CourseResponse::getReference).sorted().toList());
  }

  @Test
  void listCourses_filterByTrack_returnsTrackAndCommonCourses() {
    var trackEl = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var trackTn = seeder.track(TrackCode.TN, "Transformation Numerique");
    seeder.course("PROG1", "Algorithmique", 6, 1, trackEl);
    seeder.course("WEB1", "Interfaces web", 6, 1, null);
    seeder.course("TN1", "Marketing Digital", 3, 4, trackTn);

    var response =
        restTemplate.getForEntity("/courses?trackId=" + trackEl.getId(), CourseResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var courses = List.of(response.getBody());
    assertEquals(2, courses.size());
    assertEquals(
        List.of("PROG1", "WEB1"),
        courses.stream().map(CourseResponse::getReference).sorted().toList());
  }

  @Test
  void listCourses_filterBySemesterNumber_returnsMatchingCourses() {
    var trackEl = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    seeder.course("PROG1", "Algorithmique", 6, 1, trackEl);
    seeder.course("WEB1", "Interfaces web", 6, 1, null);
    seeder.course("THEORIE1", "Mathematiques de l'informatique", 4, 2, null);

    var response = restTemplate.getForEntity("/courses?semesterNumber=2", CourseResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var courses = List.of(response.getBody());
    assertEquals(1, courses.size());
    assertEquals("THEORIE1", courses.getFirst().getReference());
  }

  @Test
  void listCourses_filterByTrackAndSemester_returnsMatchingCourses() {
    var trackEl = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var trackTn = seeder.track(TrackCode.TN, "Transformation Numerique");
    seeder.course("PROG1", "Algorithmique", 6, 1, trackEl);
    seeder.course("PROG4", "Qualite et sureté des applications", 8, 4, null);
    seeder.course("TN1", "Marketing Digital", 3, 4, trackTn);

    var response =
        restTemplate.getForEntity(
            "/courses?trackId=" + trackTn.getId() + "&semesterNumber=4", CourseResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var courses = List.of(response.getBody());
    assertEquals(2, courses.size());
    assertTrue(courses.stream().anyMatch(c -> c.getReference().equals("PROG4")));
    assertTrue(courses.stream().anyMatch(c -> c.getReference().equals("TN1")));
  }

  @Test
  void updateCourse_updatesTitleAndCredits() {
    var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var saved = seeder.course("PROG1", "Algorithmique", 6, 1, track);

    var response =
        patch(
            "/courses/" + saved.getId(),
            new CourseUpdateRequest().title("Algorithmique avancee").credits(8),
            CourseResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var course = response.getBody();
    assertNotNull(course);
    assertEquals("PROG1", course.getReference());
    assertEquals("Algorithmique avancee", course.getTitle());
    assertEquals(8, course.getCredits());
    assertEquals(1, course.getSemesterNumber());
    assertEquals(track.getId(), Objects.requireNonNull(course.getTrack()).getId());
    var reloaded = courseRepository.findById(saved.getId()).orElseThrow();
    assertEquals("Algorithmique avancee", reloaded.getTitle());
    assertEquals(8, reloaded.getCredits());
  }

  @Test
  void updateCourse_changesTrack() {
    var trackEl = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var trackTn = seeder.track(TrackCode.TN, "Transformation Numerique");
    var saved = seeder.course("PROG1", "Algorithmique", 6, 1, trackEl);

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
    var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var saved = seeder.course("PROG1", "Algorithmique", 6, 1, track);

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
    var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var saved = seeder.course("PROG1", "Algorithmique", 6, 1, track);

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
            new CourseUpdateRequest().title("Algorithmique avancee"),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void updateCourse_invalidCredits_returnsBadRequest() {
    var saved = seeder.course("PROG1", "Algorithmique", 6, 1, null);

    var response =
        patch("/courses/" + saved.getId(), new CourseUpdateRequest().credits(0), Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  private <T> ResponseEntity<T> patch(String url, Object body, Class<T> responseType) {
    return restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body), responseType);
  }
}
