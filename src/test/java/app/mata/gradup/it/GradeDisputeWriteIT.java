package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.conf.TestDataSeeder.DisputeScenario;
import app.mata.gradup.endpoint.rest.model.DisputeStatus;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.GradeDisputeCreateRequest;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResolveRequest;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResponse;
import app.mata.gradup.repository.GradeHistoryRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGrade;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTeacher;
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

class GradeDisputeWriteIT extends SecuredFacadeIT {

  private static final String TEACHER_EMAIL = "teacher@cu.te";

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private GradeHistoryRepository gradeHistoryRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void createGradeDispute_student_returnsCreatedDispute() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    loginAsStudent(fixture.student);

    ResponseEntity<GradeDisputeResponse> response =
        restTemplate.postForEntity(
            "/grades/" + grade.getId() + "/disputes",
            new GradeDisputeCreateRequest().reason("Wrong score"),
            GradeDisputeResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    GradeDisputeResponse body = response.getBody();
    assertNotNull(body);
    assertNotNull(body.getId());
    assertEquals(grade.getId(), body.getGradeId());
    assertEquals(fixture.student.getId(), body.getStudentId());
    assertEquals("Hery Rakoto", body.getStudentName());
    assertEquals("PROG1", body.getCourseReference());
    assertEquals("Final", body.getExamLabel());
    assertEquals("Wrong score", body.getReason());
    assertEquals("PENDING", body.getStatus().toString());
    assertNull(body.getResolvedByName());
  }

  @Test
  void createGradeDispute_duplicatePending_returnsConflict() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    seeder.dispute(grade, fixture.student, "first");
    loginAsStudent(fixture.student);

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/grades/" + grade.getId() + "/disputes",
            new GradeDisputeCreateRequest().reason("second"),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void createGradeDispute_studentOnOtherGrade_returnsForbidden() {
    Fixture fixture = seedFixture();
    JStudent other = seedOtherStudent(fixture);
    JGrade otherGrade = gradeOf(fixture, other);
    loginAsStudent(fixture.student);

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/grades/" + otherGrade.getId() + "/disputes",
            new GradeDisputeCreateRequest().reason("Wrong score"),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void createGradeDispute_teacher_returnsForbidden() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    seedTeacher();
    loginAsTeacher();

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/grades/" + grade.getId() + "/disputes",
            new GradeDisputeCreateRequest().reason("Wrong score"),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void resolveDispute_admin_resolvesWithNewScoreAndLinksHistory() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    UUID disputeId = seeder.dispute(grade, fixture.student, "Wrong score").getId();

    ResponseEntity<GradeDisputeResponse> response =
        restTemplate.exchange(
            "/disputes/" + disputeId,
            HttpMethod.PATCH,
            new HttpEntity<>(
                new GradeDisputeResolveRequest()
                    .status(DisputeStatus.RESOLVED)
                    .resolutionNote("Confirmed wrong, corrected")
                    .newScore(16.0)),
            GradeDisputeResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    GradeDisputeResponse body = response.getBody();
    assertNotNull(body);
    assertEquals("RESOLVED", body.getStatus().toString());
    assertNotNull(body.getResolvedAt());
    assertEquals("Tafita Mathieu", body.getResolvedByName());
    assertEquals("Confirmed wrong, corrected", body.getResolutionNote());
    assertNotNull(body.getResultingHistoryId());
    var history = gradeHistoryRepository.findByGradeId(grade.getId());
    assertEquals(1, history.size());
    assertEquals(16.0, history.getFirst().getNewScore().doubleValue());
    assertEquals(12.0, history.getFirst().getOldScore().doubleValue());
  }

  @Test
  void resolveDispute_admin_rejectsWithoutScoreChange() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    UUID disputeId = seeder.dispute(grade, fixture.student, "Wrong score").getId();

    ResponseEntity<GradeDisputeResponse> response =
        restTemplate.exchange(
            "/disputes/" + disputeId,
            HttpMethod.PATCH,
            new HttpEntity<>(
                new GradeDisputeResolveRequest()
                    .status(DisputeStatus.REJECTED)
                    .resolutionNote("Score is correct")),
            GradeDisputeResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    GradeDisputeResponse body = response.getBody();
    assertNotNull(body);
    assertEquals("REJECTED", body.getStatus().toString());
    assertNull(body.getResultingHistoryId());
    assertEquals(
        12.0, gradeRepository.findById(grade.getId()).orElseThrow().getScore().doubleValue());
  }

  @Test
  void resolveDispute_notPending_returnsConflict() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    UUID disputeId = seeder.dispute(grade, fixture.student, "Wrong score").getId();

    ResponseEntity<GradeDisputeResponse> first =
        restTemplate.exchange(
            "/disputes/" + disputeId,
            HttpMethod.PATCH,
            new HttpEntity<>(
                new GradeDisputeResolveRequest()
                    .status(DisputeStatus.RESOLVED)
                    .resolutionNote("First resolution")),
            GradeDisputeResponse.class);
    assertEquals(HttpStatus.OK, first.getStatusCode());

    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/disputes/" + disputeId,
            HttpMethod.PATCH,
            new HttpEntity<>(
                new GradeDisputeResolveRequest()
                    .status(DisputeStatus.REJECTED)
                    .resolutionNote("Already handled")),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void resolveDispute_unknownDispute_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/disputes/" + UUID.randomUUID(),
            HttpMethod.PATCH,
            new HttpEntity<>(
                new GradeDisputeResolveRequest()
                    .status(DisputeStatus.RESOLVED)
                    .resolutionNote("Not found")),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void resolveDispute_unassignedTeacher_returnsForbidden() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    UUID disputeId = seeder.dispute(grade, fixture.student, "Wrong score").getId();
    seedTeacher();
    loginAsTeacher();

    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/disputes/" + disputeId,
            HttpMethod.PATCH,
            new HttpEntity<>(
                new GradeDisputeResolveRequest()
                    .status(DisputeStatus.RESOLVED)
                    .resolutionNote("Not allowed")),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void resolveDispute_assignedTeacher_resolves() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    UUID disputeId = seeder.dispute(grade, fixture.student, "Wrong score").getId();
    JTeacher teacher = seedTeacher();
    seeder.teacherAssignment(teacher, fixture.offering());
    loginAsTeacher();

    ResponseEntity<GradeDisputeResponse> response =
        restTemplate.exchange(
            "/disputes/" + disputeId,
            HttpMethod.PATCH,
            new HttpEntity<>(
                new GradeDisputeResolveRequest()
                    .status(DisputeStatus.RESOLVED)
                    .resolutionNote("Fixed")
                    .newScore(15.0)),
            GradeDisputeResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("RESOLVED", response.getBody().getStatus().toString());
  }

  @Test
  void listStudentDisputes_student_returnsOwnDisputes() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    seeder.dispute(grade, fixture.student, "Wrong score");
    loginAsStudent(fixture.student);

    ResponseEntity<GradeDisputeResponse[]> response =
        restTemplate.getForEntity(
            "/students/" + fixture.student.getId() + "/disputes", GradeDisputeResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().length);
    assertEquals("Wrong score", response.getBody()[0].getReason());
  }

  @Test
  void listStudentDisputes_studentOnOtherStudent_returnsForbidden() {
    Fixture fixture = seedFixture();
    gradeOf(fixture, fixture.student);
    JStudent other = seedOtherStudent(fixture);
    loginAsStudent(fixture.student);

    ResponseEntity<Error> response =
        restTemplate.getForEntity("/students/" + other.getId() + "/disputes", Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void listStudentDisputes_admin_returnsAll() {
    Fixture fixture = seedFixture();
    JGrade grade = gradeOf(fixture, fixture.student);
    seeder.dispute(grade, fixture.student, "Wrong score");

    ResponseEntity<GradeDisputeResponse[]> response =
        restTemplate.getForEntity(
            "/students/" + fixture.student.getId() + "/disputes", GradeDisputeResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().length);
  }

  @Test
  void listStudentDisputes_unknownStudent_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.getForEntity("/students/" + UUID.randomUUID() + "/disputes", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  private Fixture seedFixture() {
    return seeder.inTransaction(
        () -> {
          DisputeScenario scenario = seeder.disputeScenario();
          JStudent student =
              seeder.student(
                  "STD21001",
                  "Rakoto",
                  "Hery",
                  "std21001@cu.te",
                  scenario.cohort(),
                  scenario.track(),
                  scenario.group());
          seeder.grade(student, scenario.exam(), "12.00");
          return new Fixture(student, scenario);
        });
  }

  private JStudent seedOtherStudent(Fixture fixture) {
    JStudent other =
        seeder.inTransaction(
            () ->
                seeder.student(
                    "STD21002",
                    "Rabe",
                    "Mialy",
                    "std21002@cu.te",
                    fixture.student.getCohort(),
                    fixture.track(),
                    fixture.group()));
    seeder.inTransaction(
        () -> {
          seeder.grade(other, fixture.exam(), "14.00");
          return null;
        });
    return other;
  }

  private JGrade gradeOf(Fixture fixture, JStudent student) {
    return gradeRepository
        .findByStudentIdAndExamId(student.getId(), fixture.exam().getId())
        .orElseThrow();
  }

  private JTeacher seedTeacher() {
    return seeder.inTransaction(() -> seeder.teacher(TEACHER_EMAIL, "Rakotomalala", "Njato"));
  }

  private void loginAsTeacher() {
    loginAsUser(restTemplate, TEACHER_EMAIL);
  }

  private void loginAsStudent(JStudent student) {
    loginAsUser(restTemplate, student.getUser().getEmail());
  }

  private record Fixture(JStudent student, DisputeScenario scenario) {

    JExam exam() {
      return scenario.exam();
    }

    JCourseOffering offering() {
      return scenario.offering();
    }

    JTrack track() {
      return scenario.track();
    }

    JGroup group() {
      return scenario.group();
    }
  }
}
