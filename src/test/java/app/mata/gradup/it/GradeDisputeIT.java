package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.conf.TestDataSeeder.DisputeScenario;
import app.mata.gradup.endpoint.rest.model.DisputeStatus;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.GradeDisputePageResponse;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResolveRequest;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResponse;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JTeacher;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GradeDisputeIT extends SecuredFacadeIT {

  private static final String TEACHER_EMAIL = "teacher@cu.te";

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @Autowired private GradeRepository gradeRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void listDisputes_admin_defaultsToPending() {
    var fixture = seedFixture();
    seedDispute(fixture, "STD21001", "wrong score", 12);
    seedDispute(fixture, "STD21002", "missing partial credit", 12);

    ResponseEntity<GradeDisputePageResponse> response =
        restTemplate.getForEntity("/disputes", GradeDisputePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    GradeDisputePageResponse body = response.getBody();
    assertNotNull(body);
    assertEquals(2, body.getContent().size());
    assertEquals(2L, body.getTotalElements());
    assertEquals(0, body.getPage());
    assertEquals("PENDING", body.getContent().getFirst().getStatus().toString());
    assertNotNull(body.getContent().getFirst().getStudentName());
    assertNotNull(body.getContent().getFirst().getCourseReference());
    assertNotNull(body.getContent().getFirst().getExamLabel());
    assertNull(body.getContent().getFirst().getResolvedByName());
    assertNull(body.getContent().getFirst().getResolvedAt());
  }

  @Test
  void listDisputes_admin_filtersByStatus() {
    var fixture = seedFixture();
    seedDispute(fixture, "STD21001", "wrong score", 12);
    var resolvedId = seedDispute(fixture, "STD21002", "missing partial credit", 12);
    resolveDispute(resolvedId);

    ResponseEntity<GradeDisputePageResponse> pending =
        restTemplate.getForEntity("/disputes?status=PENDING", GradeDisputePageResponse.class);

    assertEquals(HttpStatus.OK, pending.getStatusCode());
    assertNotNull(pending.getBody());
    assertEquals(1, pending.getBody().getContent().size());
    assertEquals("wrong score", pending.getBody().getContent().getFirst().getReason());

    ResponseEntity<GradeDisputePageResponse> resolved =
        restTemplate.getForEntity("/disputes?status=RESOLVED", GradeDisputePageResponse.class);

    assertEquals(HttpStatus.OK, resolved.getStatusCode());
    assertNotNull(resolved.getBody());
    assertEquals(1, resolved.getBody().getContent().size());
    assertEquals("RESOLVED", resolved.getBody().getContent().getFirst().getStatus().toString());
    assertNotNull(resolved.getBody().getContent().getFirst().getResolvedByName());
    assertNotNull(resolved.getBody().getContent().getFirst().getResolvedAt());
  }

  @Test
  void listDisputes_admin_filtersByRejected() {
    var fixture = seedFixture();
    seedDispute(fixture, "STD21001", "wrong score", 12);
    var rejectedId = seedDispute(fixture, "STD21002", "missing partial credit", 12);
    rejectDispute(rejectedId);

    ResponseEntity<GradeDisputePageResponse> response =
        restTemplate.getForEntity("/disputes?status=REJECTED", GradeDisputePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("REJECTED", response.getBody().getContent().getFirst().getStatus().toString());
  }

  @Test
  void listDisputes_invalidStatus_returnsBadRequest() {
    var response = restTemplate.getForEntity("/disputes?status=UNKNOWN", Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void listDisputes_admin_paginates() {
    var fixture = seedFixture();
    seedDispute(fixture, "STD21001", "dispute 1", 12);
    seedDispute(fixture, "STD21002", "dispute 2", 12);
    seedDispute(fixture, "STD21003", "dispute 3", 12);

    ResponseEntity<GradeDisputePageResponse> response =
        restTemplate.getForEntity("/disputes?page=0&size=2", GradeDisputePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    GradeDisputePageResponse body = response.getBody();
    assertNotNull(body);
    assertEquals(2, body.getContent().size());
    assertEquals(3L, body.getTotalElements());
    assertEquals(2, body.getTotalPages());
    assertEquals(0, body.getPage());
    assertEquals(2, body.getSize());
    assertTrue(body.getFirst());
    assertFalse(body.getLast());
  }

  @Test
  void listDisputes_teacher_onlySeesAssignedOfferings() {
    var fixture = seedFixture();
    var otherOffering = seedAdditionalOffering(fixture);
    seedDispute(fixture, "STD21001", "assigned offering", 12);
    seedDisputeOnOffering(
        fixture, "STD21002", otherOffering.offering, otherOffering.exam, "other offering", 12);

    seedTeacherAssignedTo(fixture.offering());
    loginAsTeacher();

    ResponseEntity<GradeDisputePageResponse> response =
        restTemplate.getForEntity("/disputes", GradeDisputePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("assigned offering", response.getBody().getContent().getFirst().getReason());
  }

  @Test
  void listDisputes_teacher_filtersByStatus() {
    var fixture = seedFixture();
    seedDispute(fixture, "STD21001", "assigned pending", 12);
    var resolvedId = seedDispute(fixture, "STD21002", "assigned resolved", 12);
    resolveDispute(resolvedId);

    seedTeacherAssignedTo(fixture.offering());
    loginAsTeacher();

    ResponseEntity<GradeDisputePageResponse> response =
        restTemplate.getForEntity("/disputes?status=RESOLVED", GradeDisputePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("assigned resolved", response.getBody().getContent().getFirst().getReason());
  }

  @Test
  void listDisputes_teacher_withNoAssignment_returnsEmpty() {
    var fixture = seedFixture();
    seedDispute(fixture, "STD21001", "wrong score", 12);

    seedTeacher();
    loginAsTeacher();

    ResponseEntity<GradeDisputePageResponse> response =
        restTemplate.getForEntity("/disputes", GradeDisputePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().getContent().isEmpty());
    assertEquals(0L, response.getBody().getTotalElements());
  }

  @Test
  void listDisputes_student_returnsForbidden() {
    var fixture = seedFixture();
    seedDispute(fixture, "STD21001", "wrong score", 12);
    seedUser("student@cu.te", Role.STUDENT);
    loginAsUser(restTemplate, "student@cu.te");

    ResponseEntity<Error> response = restTemplate.getForEntity("/disputes", Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void listDisputes_anonymous_returnsUnauthorized() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();

    ResponseEntity<Error> response = restTemplate.getForEntity("/disputes", Error.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UNAUTHORIZED", response.getBody().getCode());
  }

  private DisputeScenario seedFixture() {
    return seeder.disputeScenario();
  }

  private JTeacher seedTeacher() {
    return seeder.inTransaction(() -> seeder.teacher(TEACHER_EMAIL, "Rakotomalala", "Njato"));
  }

  private void seedTeacherAssignedTo(JCourseOffering offering) {
    seeder.inTransaction(
        () -> {
          JTeacher teacher = seedTeacher();
          seeder.teacherAssignment(teacher, offering);
          return null;
        });
  }

  private void loginAsTeacher() {
    loginAsUser(restTemplate, TEACHER_EMAIL);
  }

  private UUID seedDispute(DisputeScenario fixture, String reference, String reason, int score) {
    return seedDisputeOnOffering(
        fixture, reference, fixture.offering(), fixture.exam(), reason, score);
  }

  private UUID seedDisputeOnOffering(
      DisputeScenario fixture,
      String reference,
      JCourseOffering offering,
      JExam exam,
      String reason,
      int score) {
    return seeder.inTransaction(
        () -> {
          var student =
              seeder.student(
                  reference,
                  "Rakoto",
                  "Hery",
                  reference + "@cu.te",
                  fixture.cohort(),
                  fixture.track(),
                  fixture.group());
          seeder.grade(student, exam, score + ".00");
          var grade =
              gradeRepository.findByStudentIdAndExamId(student.getId(), exam.getId()).orElseThrow();
          return seeder.dispute(grade, student, reason).getId();
        });
  }

  private void resolveDispute(UUID disputeId) {
    patchDispute(disputeId, DisputeStatus.RESOLVED, "Confirmed, corrected");
  }

  private void rejectDispute(UUID disputeId) {
    patchDispute(disputeId, DisputeStatus.REJECTED, "Score is correct");
  }

  private void patchDispute(UUID disputeId, DisputeStatus status, String resolutionNote) {
    var response =
        restTemplate.exchange(
            "/disputes/" + disputeId,
            HttpMethod.PATCH,
            new HttpEntity<>(
                new GradeDisputeResolveRequest().status(status).resolutionNote(resolutionNote)),
            GradeDisputeResponse.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  private OfferingSeed seedAdditionalOffering(DisputeScenario fixture) {
    return seeder.inTransaction(
        () -> {
          var course = seeder.course("PROG2", 6, 2, fixture.track());
          var offering = seeder.offering(course, fixture.group(), fixture.semester());
          var exam = seeder.exam(offering);
          return new OfferingSeed(offering, exam);
        });
  }

  private record OfferingSeed(JCourseOffering offering, JExam exam) {}
}
