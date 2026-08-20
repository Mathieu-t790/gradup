package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTeacher;
import java.time.LocalDate;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

class TeacherWebIT extends SecuredFacadeIT {

  private static final Pattern CSRF_PATTERN =
      Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");
  private static final Pattern GRADE_ID_PATTERN =
      Pattern.compile("/teacher/courses/grades/([0-9a-f-]+)");

  private record Fixture(
      JCourseOffering offering, JExam exam, JStudent student, JTeacher teacher) {}

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
  }

  private JCourseOffering seedOffering(String courseReference) {
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
    var el = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var group = seeder.group("G1", cohort, el);
    var year =
        seeder.academicYear("2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
    var s1 = seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
    var prog = seeder.course(courseReference, 60, 1, null);
    return seeder.offering(prog, group, s1);
  }

  private Fixture seedFixture() {
    var offering = seedOffering("PROG1");
    var exam = seeder.exam(offering);
    var student =
        seeder.student(
            "STD21001",
            "Rakoto",
            "Hery",
            "hery@cu.te",
            offering.getGroup().getCohort(),
            offering.getGroup().getTrack(),
            offering.getGroup());
    var teacher = seeder.teacher("teacher.web@cu.te", "Rakoto", "Tia");
    seeder.teacherAssignment(teacher, offering);
    return new Fixture(offering, exam, student, teacher);
  }

  private String csrfFrom(ResponseEntity<String> page) {
    var body = page.getBody();
    if (body != null) {
      var matcher = CSRF_PATTERN.matcher(body);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    return csrfToken(restTemplate);
  }

  private ResponseEntity<String> postForm(String url, String... keyValues) {
    var csrf = csrfFrom(restTemplate.getForEntity("/teacher/courses", String.class));
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("_csrf", csrf);
    for (int i = 0; i + 1 < keyValues.length; i += 2) {
      formData.add(keyValues[i], keyValues[i + 1]);
    }
    return restTemplate.postForEntity(url, new HttpEntity<>(formData, headers), String.class);
  }

  @Test
  void teacher_sees_only_assigned_courses() {
    var offering = seedOffering("PROG1");
    var other = seedOffering("PROG2");
    var teacher = seeder.teacher("teacher.web@cu.te", "Rakoto", "Tia");
    seeder.teacherAssignment(teacher, offering);
    loginAsUser(restTemplate, "teacher.web@cu.te");

    var response = restTemplate.getForEntity("/teacher/courses", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("PROG1"));
    assertTrue(response.getBody().contains("Saisir les notes"));
    assertTrue(!response.getBody().contains("PROG2"));
  }

  @Test
  void teacher_records_then_updates_grade() {
    var fixture = seedFixture();
    loginAsUser(restTemplate, "teacher.web@cu.te");

    var page =
        restTemplate.getForEntity("/teacher/courses/" + fixture.offering().getId(), String.class);

    assertEquals(HttpStatus.OK, page.getStatusCode());
    assertNotNull(page.getBody());
    assertTrue(page.getBody().contains("STD21001"));
    assertTrue(page.getBody().contains("Hery Rakoto"));

    var create =
        postForm(
            "/teacher/courses/"
                + fixture.offering().getId()
                + "/exams/"
                + fixture.exam().getId()
                + "/grades",
            "studentId",
            fixture.student().getId().toString(),
            "score",
            "12");

    assertEquals(HttpStatus.FOUND, create.getStatusCode());
    assertTrue(create.getHeaders().getLocation().toString().contains("/teacher/courses/"));

    var afterCreate =
        restTemplate.getForEntity("/teacher/courses/" + fixture.offering().getId(), String.class);
    assertNotNull(afterCreate.getBody());
    assertTrue(afterCreate.getBody().contains("12.0"));

    var gradeIdMatcher = GRADE_ID_PATTERN.matcher(afterCreate.getBody());
    assertTrue(gradeIdMatcher.find(), "grade id not found in page");
    String gradeId = gradeIdMatcher.group(1);

    var update = postForm("/teacher/courses/grades/" + gradeId, "score", "15");

    assertEquals(HttpStatus.FOUND, update.getStatusCode());

    var afterUpdate =
        restTemplate.getForEntity("/teacher/courses/" + fixture.offering().getId(), String.class);
    assertNotNull(afterUpdate.getBody());
    assertTrue(afterUpdate.getBody().contains("15.0"));
  }

  @Test
  void record_grade_rejects_invalid_score() {
    var fixture = seedFixture();
    loginAsUser(restTemplate, "teacher.web@cu.te");

    var response =
        postForm(
            "/teacher/courses/"
                + fixture.offering().getId()
                + "/exams/"
                + fixture.exam().getId()
                + "/grades",
            "studentId",
            fixture.student().getId().toString(),
            "score",
            "abc");

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertTrue(response.getHeaders().getLocation().toString().contains("error=score.invalid"));

    var page =
        restTemplate.getForEntity("/teacher/courses/" + fixture.offering().getId(), String.class);
    assertNotNull(page.getBody());
    assertTrue(page.getBody().contains("Note invalide"));
  }

  @Test
  void teacher_cannot_access_another_teachers_offering() {
    var fixture = seedFixture();
    seeder.teacher("other.teacher@cu.te", "Rabe", "Mialy");
    loginAsUser(restTemplate, "other.teacher@cu.te");

    var page =
        restTemplate.getForEntity("/teacher/courses/" + fixture.offering().getId(), String.class);

    assertEquals(HttpStatus.FORBIDDEN, page.getStatusCode());

    var post =
        postForm(
            "/teacher/courses/"
                + fixture.offering().getId()
                + "/exams/"
                + fixture.exam().getId()
                + "/grades",
            "studentId",
            fixture.student().getId().toString(),
            "score",
            "12");

    assertEquals(HttpStatus.FORBIDDEN, post.getStatusCode());
  }

  @Test
  void teacher_courses_page_is_forbidden_for_student() {
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
    var el = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var group = seeder.group("G1", cohort, el);
    seeder.student("STD21001", "Rakoto", "Hery", "hery@cu.te", cohort, el, group);
    loginAsUser(restTemplate, "hery@cu.te");

    var response = restTemplate.getForEntity("/teacher/courses", String.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void teacher_courses_page_is_forbidden_for_admin() {
    seedUser("admin.web@cu.te", Role.ADMIN);
    loginAsUser(restTemplate, "admin.web@cu.te");

    var response = restTemplate.getForEntity("/teacher/courses", String.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void teacher_home_redirects_to_courses() {
    seeder.teacher("teacher.web@cu.te", "Rakoto", "Tia");
    loginAsUser(restTemplate, "teacher.web@cu.te");

    var response = restTemplate.getForEntity("/", String.class);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertTrue(response.getHeaders().getLocation().toString().contains("/teacher/courses"));
  }
}
