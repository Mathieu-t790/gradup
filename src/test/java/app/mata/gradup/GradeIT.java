package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.GradeHistoryEntryResponse;
import app.mata.gradup.endpoint.rest.model.GradeResponse;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JTrack;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GradeIT extends SecuredFacadeIT {

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
  void listExamGrades_returnsGradesSortedByStudentName() {
    var fixture = seedOffering();
    var exam = fixture.exam();
    UUID andriaId =
        seeder.inTransaction(
            () -> {
              var andria =
                  seeder.student(
                      "STD21003",
                      "Andria",
                      "Tiana",
                      "andria@cu.te",
                      fixture.cohort(),
                      fixture.track(),
                      fixture.group());
              var rakoto =
                  seeder.student(
                      "STD21001",
                      "Rakoto",
                      "Hery",
                      "rakoto@cu.te",
                      fixture.cohort(),
                      fixture.track(),
                      fixture.group());
              seeder.grade(rakoto, exam, "12.00");
              seeder.grade(andria, exam, "15.00");
              return andria.getId();
            });

    ResponseEntity<GradeResponse[]> response =
        restTemplate.getForEntity("/exams/" + exam.getId() + "/grades", GradeResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(2, response.getBody().length);
    assertEquals("Tiana Andria", response.getBody()[0].getStudentName());
    assertEquals("Hery Rakoto", response.getBody()[1].getStudentName());
    GradeResponse first = response.getBody()[0];
    assertEquals(andriaId, first.getStudentId());
    assertEquals(exam.getId(), first.getExamId());
    assertEquals("Final", first.getExamLabel());
    assertEquals("PROG1", first.getCourseReference());
    assertEquals(15.0, first.getScore());
    assertNotNull(first.getRecordedAt());
    assertEquals("Tafita Mathieu", first.getRecordedByName());
  }

  @Test
  void listExamGrades_unknownExam_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.getForEntity("/exams/" + UUID.randomUUID() + "/grades", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void listGradeHistory_returnsChronologicalEntries() {
    var fixture = seedOffering();
    var exam = fixture.exam();
    UUID gradeId =
        seeder.inTransaction(
            () -> {
              var student =
                  seeder.student(
                      "STD21001",
                      "Rakoto",
                      "Hery",
                      "hery@cu.te",
                      fixture.cohort(),
                      fixture.track(),
                      fixture.group());
              seeder.grade(student, exam, "12.00");
              return gradeRepository
                  .findByStudentIdAndExamId(student.getId(), exam.getId())
                  .orElseThrow()
                  .getId();
            });
    seeder.changeScore("STD21001", "PROG1", new BigDecimal("14.00"));
    seeder.changeScore("STD21001", "PROG1", new BigDecimal("16.00"));

    ResponseEntity<GradeHistoryEntryResponse[]> response =
        restTemplate.getForEntity(
            "/grades/" + gradeId + "/history", GradeHistoryEntryResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(2, response.getBody().length);
    GradeHistoryEntryResponse first = response.getBody()[0];
    assertEquals(12.0, first.getOldScore());
    assertEquals(14.0, first.getNewScore());
    GradeHistoryEntryResponse second = response.getBody()[1];
    assertEquals(14.0, second.getOldScore());
    assertEquals(16.0, second.getNewScore());
    assertEquals("Tafita Mathieu", first.getModifiedByName());
    assertNotNull(first.getModifiedAt());
    assertNotNull(first.getReason());
  }

  @Test
  void listGradeHistory_unknownGrade_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.getForEntity("/grades/" + UUID.randomUUID() + "/history", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void listExamGrades_teacherNotAssigned_returnsForbidden() {
    var fixture = seedOffering();
    var exam = fixture.exam();
    seedUser("teacher@cu.te", Role.TEACHER);
    loginAsUser(restTemplate, "teacher@cu.te");

    ResponseEntity<Error> response =
        restTemplate.getForEntity("/exams/" + exam.getId() + "/grades", Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void listGradeHistory_studentOwner_returnsEntries() {
    var fixture = seedOffering();
    var exam = fixture.exam();
    UUID gradeId =
        seeder.inTransaction(
            () -> {
              var student =
                  seeder.student(
                      "STD21001",
                      "Rakoto",
                      "Hery",
                      "hery@cu.te",
                      fixture.cohort(),
                      fixture.track(),
                      fixture.group());
              seeder.grade(student, exam, "12.00");
              return gradeRepository
                  .findByStudentIdAndExamId(student.getId(), exam.getId())
                  .orElseThrow()
                  .getId();
            });
    loginAsUser(restTemplate, "hery@cu.te");

    ResponseEntity<GradeHistoryEntryResponse[]> response =
        restTemplate.getForEntity(
            "/grades/" + gradeId + "/history", GradeHistoryEntryResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().length);
  }

  @Test
  void listGradeHistory_studentNotOwner_returnsForbidden() {
    var fixture = seedOffering();
    var exam = fixture.exam();
    UUID gradeId =
        seeder.inTransaction(
            () -> {
              var owner =
                  seeder.student(
                      "STD21001",
                      "Rakoto",
                      "Hery",
                      "hery@cu.te",
                      fixture.cohort(),
                      fixture.track(),
                      fixture.group());
              seeder.grade(owner, exam, "12.00");
              seeder.student(
                  "STD21002",
                  "Rabe",
                  "Mialy",
                  "mialy@cu.te",
                  fixture.cohort(),
                  fixture.track(),
                  fixture.group());
              return gradeRepository
                  .findByStudentIdAndExamId(owner.getId(), exam.getId())
                  .orElseThrow()
                  .getId();
            });
    loginAsUser(restTemplate, "mialy@cu.te");

    ResponseEntity<Error> response =
        restTemplate.getForEntity("/grades/" + gradeId + "/history", Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  private Fixture seedOffering() {
    return seeder.inTransaction(
        () -> {
          var year =
              seeder.academicYear("2024-2025", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 8, 31));
          var semester =
              seeder.semester(1, year, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 31));
          var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
          var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
          var group = seeder.group("K1", cohort, track);
          var course = seeder.course("PROG1", 6, 1, track);
          var offering = seeder.offering(course, group, semester);
          var exam = seeder.exam(offering);
          return new Fixture(cohort, track, group, exam);
        });
  }

  private record Fixture(JCohort cohort, JTrack track, JGroup group, JExam exam) {}
}
