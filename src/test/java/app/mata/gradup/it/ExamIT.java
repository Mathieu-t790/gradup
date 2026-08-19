package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ExamIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void getExam_admin_returnsExam() {
    var exam = seedExam("Final", LocalTime.of(9, 30));

    ResponseEntity<ExamResponse> response =
        restTemplate.getForEntity("/exams/" + exam.getId(), ExamResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    ExamResponse body = response.getBody();
    assertNotNull(body);
    assertEquals(exam.getId(), body.getId());
    assertEquals(exam.getOffering().getId(), body.getOfferingId());
    assertEquals("Final", body.getLabel());
    assertEquals(LocalDate.of(2022, 5, 30), body.getExamDate());
    assertEquals("09:30:00", body.getExamTime());
    assertEquals(1, body.getWeightNumerator());
    assertEquals(1, body.getWeightDenominator());
  }

  @Test
  void getExam_teacherRole_returnsExam() {
    var exam = seedExam("Midterm", null);
    seedUser("teacher@cu.te", Role.TEACHER);
    loginAsUser(restTemplate, "teacher@cu.te");

    ResponseEntity<ExamResponse> response =
        restTemplate.getForEntity("/exams/" + exam.getId(), ExamResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(exam.getId(), response.getBody().getId());
    assertNull(response.getBody().getExamTime());
  }

  @Test
  void getExam_studentRole_returnsExam() {
    var exam = seedExam("Final", null);
    seedUser("student@cu.te", Role.STUDENT);
    loginAsUser(restTemplate, "student@cu.te");

    ResponseEntity<ExamResponse> response =
        restTemplate.getForEntity("/exams/" + exam.getId(), ExamResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(exam.getId(), response.getBody().getId());
  }

  @Test
  void getExam_unknownExam_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.getForEntity("/exams/" + UUID.randomUUID(), Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void getExam_anonymous_returnsUnauthorized() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    ResponseEntity<Error> response =
        restTemplate.getForEntity("/exams/" + UUID.randomUUID(), Error.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UNAUTHORIZED", response.getBody().getCode());
  }

  private JExam seedExam(String label, LocalTime examTime) {
    return seeder.exam(seedOffering(), label, LocalDate.of(2022, 5, 30), examTime, 1, 1);
  }

  private JCourseOffering seedOffering() {
    var base = seeder.semesterScenario();
    return seeder.offering(
        seeder.course("PROG1", 6, 1, base.track()), base.group(), base.semester());
  }
}
