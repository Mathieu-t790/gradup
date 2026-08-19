package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.GradeCreateRequest;
import app.mata.gradup.endpoint.rest.model.GradeResponse;
import app.mata.gradup.endpoint.rest.model.GradeUpdateRequest;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.GradeHistoryRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGrade;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTrack;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GradeWriteIT extends SecuredFacadeIT {

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
  void recordGrade_returnsCreatedGrade() {
    Fixture fixture = seedFixture();
    var student = fixture.student;

    ResponseEntity<GradeResponse> response =
        restTemplate.postForEntity(
            "/exams/" + fixture.exam.getId() + "/grades",
            new GradeCreateRequest().studentId(student.getId()).score(14.5),
            GradeResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    GradeResponse body = response.getBody();
    assertNotNull(body);
    assertNotNull(body.getId());
    assertEquals(student.getId(), body.getStudentId());
    assertEquals(fixture.exam.getId(), body.getExamId());
    assertEquals("PROG1", body.getCourseReference());
    assertEquals(14.5, body.getScore());
    assertEquals("Tafita Mathieu", body.getRecordedByName());
  }

  @Test
  void recordGrade_duplicate_returnsConflict() {
    Fixture fixture = seedFixture();
    seeder.inTransaction(
        () -> {
          seeder.grade(fixture.student, fixture.exam, "12.00");
          return null;
        });

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/exams/" + fixture.exam.getId() + "/grades",
            new GradeCreateRequest().studentId(fixture.student.getId()).score(14.5),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void recordGrade_unknownExam_returnsNotFound() {
    Fixture fixture = seedFixture();

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/exams/" + UUID.randomUUID() + "/grades",
            new GradeCreateRequest().studentId(fixture.student.getId()).score(14.5),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void recordGrade_unassignedTeacher_returnsForbidden() {
    Fixture fixture = seedFixture();
    seeder.teacher("teacher@cu.te", "Mathieu", "Tafita");
    loginAsUser(restTemplate, "teacher@cu.te");

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/exams/" + fixture.exam.getId() + "/grades",
            new GradeCreateRequest().studentId(fixture.student.getId()).score(14.5),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void updateGrade_returnsUpdatedGradeAndArchivesHistory() {
    Fixture fixture = seedFixture();
    JGrade grade =
        seeder.inTransaction(
            () -> {
              seeder.grade(fixture.student, fixture.exam, "12.00");
              return gradeRepository
                  .findByStudentIdAndExamId(fixture.student.getId(), fixture.exam.getId())
                  .orElseThrow();
            });

    ResponseEntity<GradeResponse> response =
        restTemplate.exchange(
            "/grades/" + grade.getId(),
            HttpMethod.PUT,
            new HttpEntity<>(new GradeUpdateRequest().score(16.0).reason("Rechecking paper")),
            GradeResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    GradeResponse body = response.getBody();
    assertNotNull(body);
    assertEquals(16.0, body.getScore());

    var history = gradeHistoryRepository.findByGradeId(grade.getId());
    assertEquals(1, history.size());
    assertEquals(12.0, history.getFirst().getOldScore().doubleValue());
    assertEquals(16.0, history.getFirst().getNewScore().doubleValue());
    assertEquals("Rechecking paper", history.getFirst().getReason());
  }

  @Test
  void updateGrade_unknownGrade_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/grades/" + UUID.randomUUID(),
            HttpMethod.PUT,
            new HttpEntity<>(new GradeUpdateRequest().score(16.0).reason("Rechecking paper")),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void updateGrade_unassignedTeacher_returnsForbidden() {
    Fixture fixture = seedFixture();
    JGrade grade =
        seeder.inTransaction(
            () -> {
              seeder.grade(fixture.student, fixture.exam, "12.00");
              return gradeRepository
                  .findByStudentIdAndExamId(fixture.student.getId(), fixture.exam.getId())
                  .orElseThrow();
            });
    seeder.teacher("teacher@cu.te", "Mathieu", "Tafita");
    loginAsUser(restTemplate, "teacher@cu.te");

    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/grades/" + grade.getId(),
            HttpMethod.PUT,
            new HttpEntity<>(new GradeUpdateRequest().score(16.0).reason("Rechecking paper")),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  private Fixture seedFixture() {
    return seeder.inTransaction(
        () -> {
          JCohort cohort = seeder.cohort("Mpamakilay", 2021, 2024);
          JTrack track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
          JGroup group = seeder.group("K1", cohort, track);
          JAcademicYear year =
              seeder.academicYear("2024-2025", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 8, 31));
          JSemester semester =
              seeder.semester(1, year, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 31));
          JCourse course = seeder.course("PROG1", 6, 1, track);
          JCourseOffering offering = seeder.offering(course, group, semester);
          JExam exam = seeder.exam(offering);
          JStudent student =
              seeder.student("STD21001", "Rakoto", "Hery", "rakoto@cu.te", cohort, track, group);
          return new Fixture(student, exam, group, semester);
        });
  }

  private record Fixture(JStudent student, JExam exam, JGroup group, JSemester semester) {}
}
