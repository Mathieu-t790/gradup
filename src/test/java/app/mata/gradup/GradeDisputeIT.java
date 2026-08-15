package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.GradeDisputePageResponse;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GradeDisputeRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGradeDispute;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTeacher;
import app.mata.gradup.repository.model.JTrack;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GradeDisputeIT extends SecuredFacadeIT {

  private static final String TEACHER_EMAIL = "teacher@cu.te";

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @Autowired private AcademicYearRepository academicYearRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private TrackRepository trackRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private GradeDisputeRepository gradeDisputeRepository;

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
    assertEquals(false, body.getLast());
  }

  @Test
  void listDisputes_teacher_onlySeesAssignedOfferings() {
    var fixture = seedFixture();
    var otherOffering = seedAdditionalOffering(fixture);
    seedDispute(fixture, "STD21001", "assigned offering", 12);
    seedDisputeOnOffering(fixture, "STD21002", otherOffering, "other offering", 12);

    seedTeacherAssignedTo(fixture.offering);
    loginAsTeacher();

    ResponseEntity<GradeDisputePageResponse> response =
        restTemplate.getForEntity("/disputes", GradeDisputePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("assigned offering", response.getBody().getContent().getFirst().getReason());
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
    loginAs(restTemplate, "student@cu.te");

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
    var user = userRepository.findByEmail(TEACHER_EMAIL).orElseThrow();
    user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
    userRepository.save(user);
    loginAs(restTemplate, TEACHER_EMAIL);
  }

  private UUID seedDispute(Fixture fixture, String reference, String reason, int score) {
    return seedDisputeOnOffering(fixture, reference, fixture.offering, reason, score);
  }

  private UUID seedDisputeOnOffering(
      Fixture fixture, String reference, JCourseOffering offering, String reason, int score) {
    return seeder.inTransaction(
        () -> {
          JExam exam = examRepository.findByOfferingId(offering.getId()).getFirst();
          JStudent student =
              seeder.student(
                  reference,
                  "Rakoto",
                  "Hery",
                  reference + "@cu.te",
                  fixture.cohort,
                  fixture.track,
                  fixture.group);
          seeder.grade(student, exam, score + ".00");
          var grade =
              gradeRepository.findByStudentIdAndExamId(student.getId(), exam.getId()).orElseThrow();
          return seeder.dispute(grade, student, reason).getId();
        });
  }

  private void resolveDispute(UUID disputeId) {
    seeder.inTransaction(
        () -> {
          JGradeDispute dispute = gradeDisputeRepository.findById(disputeId).orElseThrow();
          dispute.setStatus(app.mata.gradup.model.DisputeStatus.RESOLVED);
          dispute.setResolvedAt(Instant.now());
          dispute.setResolvedBy(seeder.adminUserId());
          gradeDisputeRepository.save(dispute);
          return null;
        });
  }

  private Fixture seedFixture() {
    return seeder.inTransaction(
        () -> {
          JAcademicYear year =
              academicYearRepository.save(
                  JAcademicYear.builder()
                      .label("2024-2025")
                      .startDate(LocalDate.of(2024, 9, 1))
                      .endDate(LocalDate.of(2025, 8, 31))
                      .build());
          JSemester semester =
              semesterRepository.save(
                  JSemester.builder()
                      .number(1)
                      .academicYear(year)
                      .startDate(LocalDate.of(2024, 9, 1))
                      .endDate(LocalDate.of(2025, 1, 31))
                      .build());
          JCohort cohort =
              cohortRepository.save(
                  JCohort.builder()
                      .label("Mpamakilay")
                      .entryYear(2021)
                      .expectedGraduationYear(2024)
                      .build());
          JTrack track =
              trackRepository.save(
                  JTrack.builder()
                      .code(app.mata.gradup.model.TrackCode.EL)
                      .label("Ecosysteme Logiciel")
                      .build());
          JGroup group =
              groupRepository.save(
                  JGroup.builder().reference("K1").cohort(cohort).track(track).build());
          JCourse course = seeder.course("Pro1", 5, 1, track);
          JCourseOffering offering = seeder.offering(course, group, semester);
          examRepository.save(
              JExam.builder()
                  .offering(offering)
                  .label("Final")
                  .examDate(LocalDate.of(2022, 5, 30))
                  .weightNumerator(1)
                  .weightDenominator(1)
                  .build());
          return new Fixture(cohort, track, group, offering);
        });
  }

  private JCourseOffering seedAdditionalOffering(Fixture fixture) {
    return seeder.inTransaction(
        () -> {
          JSemester managedSemester =
              semesterRepository.findById(fixture.offering.getSemester().getId()).orElseThrow();
          JCourse course = seeder.course("Pro2", 5, 2, fixture.track);
          JCourseOffering offering = seeder.offering(course, fixture.group, managedSemester);
          examRepository.save(
              JExam.builder()
                  .offering(offering)
                  .label("Final")
                  .examDate(LocalDate.of(2022, 6, 30))
                  .weightNumerator(1)
                  .weightDenominator(1)
                  .build());
          return offering;
        });
  }

  private record Fixture(JCohort cohort, JTrack track, JGroup group, JCourseOffering offering) {}
}
