package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.CourseOfferingCreateRequest;
import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JTeacher;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CourseOfferingIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @Autowired private TeacherAssignmentRepository teacherAssignmentRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void getCourseOffering_returnsDetailedOffering() {
    var offering = seedOffering();

    ResponseEntity<CourseOfferingResponse> response =
        restTemplate.getForEntity(
            "/course-offerings/" + offering.getId(), CourseOfferingResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    CourseOfferingResponse body = response.getBody();
    assertNotNull(body);
    assertEquals(offering.getId(), body.getId());
    assertEquals("PROG1", body.getCourse().getReference());
    assertEquals("K1", body.getGroup().getReference());
    assertEquals(1, body.getSemester().getNumber());
    assertEquals("2024-2025", body.getSemester().getAcademicYearLabel());
    assertTrue(body.getTeachers().isEmpty());
    assertTrue(body.getGradingFinalized());
  }

  @Test
  void getCourseOffering_unknownOffering_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.getForEntity("/course-offerings/" + UUID.randomUUID(), Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void assignTeacher_returnsNoContentAndAddsAssignment() {
    var offering = seedOffering();
    var teacher = seedTeacher();

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/course-offerings/" + offering.getId() + "/teachers/" + teacher.getId(),
            HttpMethod.POST,
            null,
            Void.class);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertTrue(
        teacherAssignmentRepository.existsByTeacherIdAndOfferingId(
            teacher.getId(), offering.getId()));
  }

  @Test
  void assignTeacher_duplicate_returnsConflict() {
    var offering = seedOffering();
    var teacher = seedTeacher();
    seedAssignment(teacher, offering);

    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/course-offerings/" + offering.getId() + "/teachers/" + teacher.getId(),
            HttpMethod.POST,
            null,
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void assignTeacher_unknownOffering_returnsNotFound() {
    var teacher = seedTeacher();

    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/course-offerings/" + UUID.randomUUID() + "/teachers/" + teacher.getId(),
            HttpMethod.POST,
            null,
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assignTeacher_unknownTeacher_returnsNotFound() {
    var offering = seedOffering();

    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/course-offerings/" + offering.getId() + "/teachers/" + UUID.randomUUID(),
            HttpMethod.POST,
            null,
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assignTeacher_teacherRole_cannotAssign() {
    var offering = seedOffering();
    var teacher = seedTeacher();
    seedUser("teacher@cu.te", Role.TEACHER);
    loginAsUser(restTemplate, "teacher@cu.te");

    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/course-offerings/" + offering.getId() + "/teachers/" + teacher.getId(),
            HttpMethod.POST,
            null,
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertFalse(
        teacherAssignmentRepository.existsByTeacherIdAndOfferingId(
            teacher.getId(), offering.getId()));
  }

  @Test
  void unassignTeacher_returnsNoContentAndRemovesAssignment() {
    var offering = seedOffering();
    var teacher = seedTeacher();
    seedAssignment(teacher, offering);

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/course-offerings/" + offering.getId() + "/teachers/" + teacher.getId(),
            HttpMethod.DELETE,
            null,
            Void.class);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertFalse(
        teacherAssignmentRepository.existsByTeacherIdAndOfferingId(
            teacher.getId(), offering.getId()));
  }

  @Test
  void unassignTeacher_notAssigned_returnsNoContent() {
    var offering = seedOffering();
    var teacher = seedTeacher();

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/course-offerings/" + offering.getId() + "/teachers/" + teacher.getId(),
            HttpMethod.DELETE,
            null,
            Void.class);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void unassignTeacher_unknownOffering_returnsNotFound() {
    var teacher = seedTeacher();

    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/course-offerings/" + UUID.randomUUID() + "/teachers/" + teacher.getId(),
            HttpMethod.DELETE,
            null,
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void listOfferingExams_returnsExams() {
    var offering = seedOffering();
    seedExam(offering, LocalDate.of(2025, 1, 15), LocalTime.of(9, 0, 0), 2);

    ResponseEntity<ExamResponse[]> response =
        restTemplate.getForEntity(
            "/course-offerings/" + offering.getId() + "/exams", ExamResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().length);
    ExamResponse exam = response.getBody()[0];
    assertEquals("Final exam", exam.getLabel());
    assertEquals(offering.getId(), exam.getOfferingId());
    assertEquals(LocalDate.of(2025, 1, 15), exam.getExamDate());
    assertEquals("09:00:00", exam.getExamTime());
    assertEquals(1, exam.getWeightNumerator());
    assertEquals(2, exam.getWeightDenominator());
  }

  @Test
  void listOfferingExams_returnsEmptyList_whenNone() {
    var offering = seedOffering();

    ResponseEntity<ExamResponse[]> response =
        restTemplate.getForEntity(
            "/course-offerings/" + offering.getId() + "/exams", ExamResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().length);
  }

  @Test
  void listOfferingExams_unknownOffering_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.getForEntity("/course-offerings/" + UUID.randomUUID() + "/exams", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void listOfferingExams_studentRole_canList() {
    var offering = seedOffering();
    seedExam(offering, null, null, 1);
    seedUser("student@cu.te", Role.STUDENT);
    loginAsUser(restTemplate, "student@cu.te");

    ResponseEntity<ExamResponse[]> response =
        restTemplate.getForEntity(
            "/course-offerings/" + offering.getId() + "/exams", ExamResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().length);
    assertNull(response.getBody()[0].getExamTime());
  }

  @Test
  void createCourseOffering_commonGroup_appliesSemesterCap() {
    var fixture =
        seeder.inTransaction(
            () -> {
              var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
              var commonGroup = seeder.group("K1", cohort, null);
              var year =
                  seeder.academicYear(
                      "2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
              var semester =
                  seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
              var fullCourse = seeder.course("COMMON1", 30, 1, null);
              seeder.offering(fullCourse, commonGroup, semester);
              return new CapFixture(commonGroup.getId(), semester.getId(), null);
            });
    var extra = seeder.course("EXTRA", 3, 1, null);
    var request =
        new CourseOfferingCreateRequest()
            .courseId(extra.getId())
            .groupId(fixture.groupId())
            .semesterId(fixture.semesterId());

    ResponseEntity<Error> response =
        restTemplate.postForEntity("/course-offerings", request, Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().getMessage().contains("would exceed"));
  }

  @Test
  void createCourseOffering_sameCourseAlreadyOffered_otherGroup_allowed() {
    var fixture =
        seeder.inTransaction(
            () -> {
              var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
              var el = seeder.track("EL", "Ecosysteme Logiciel");
              var tn = seeder.track("TN", "Transformation Numerique");
              var groupEl = seeder.group("K1", cohort, el);
              var groupTn = seeder.group("K2", cohort, tn);
              var year =
                  seeder.academicYear(
                      "2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
              var semester =
                  seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
              var commonCourse = seeder.course("COMMON", 30, 1, null);
              seeder.offering(commonCourse, groupEl, semester);
              return new CapFixture(groupTn.getId(), semester.getId(), commonCourse.getId());
            });
    var request =
        new CourseOfferingCreateRequest()
            .courseId(fixture.courseId())
            .groupId(fixture.groupId())
            .semesterId(fixture.semesterId());

    ResponseEntity<CourseOfferingResponse> response =
        restTemplate.postForEntity("/course-offerings", request, CourseOfferingResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void createCourseOffering_newCourseExceedsYearCap() {
    var fixture =
        seeder.inTransaction(
            () -> {
              var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
              var el = seeder.track("EL", "Ecosysteme Logiciel");
              var group = seeder.group("K1", cohort, el);
              var year =
                  seeder.academicYear(
                      "2021-2022", LocalDate.of(2021, 9, 1), LocalDate.of(2022, 8, 31));
              var s1 =
                  seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
              var s2 =
                  seeder.semester(2, year, LocalDate.of(2022, 2, 1), LocalDate.of(2022, 6, 30));
              var s3 =
                  seeder.semester(3, year, LocalDate.of(2022, 7, 1), LocalDate.of(2022, 8, 31));
              var c1 = seeder.course("C1", 30, 1, el);
              var c2 = seeder.course("C2", 30, 2, el);
              var c3 = seeder.course("C3", 24, 3, el);
              seeder.offering(c1, group, s1);
              seeder.offering(c2, group, s2);
              seeder.offering(c3, group, s3);
              var extra = seeder.course("EXTRA", 6, 3, el);
              return new CapFixture(group.getId(), s3.getId(), extra.getId());
            });
    var request =
        new CourseOfferingCreateRequest()
            .courseId(fixture.courseId())
            .groupId(fixture.groupId())
            .semesterId(fixture.semesterId());

    ResponseEntity<Error> response =
        restTemplate.postForEntity("/course-offerings", request, Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().getMessage().contains("would exceed"));
  }

  private JCourseOffering seedOffering() {
    var base = seeder.semesterScenario();
    return seeder.offering(
        seeder.course("PROG1", 6, 1, base.track()), base.group(), base.semester());
  }

  private JTeacher seedTeacher() {
    return seeder.teacher("tafita@cu.te", "Mathieu", "Tafita");
  }

  private void seedAssignment(JTeacher teacher, JCourseOffering offering) {
    seeder.teacherAssignment(teacher, offering);
  }

  private void seedExam(
      JCourseOffering offering, LocalDate examDate, LocalTime examTime, int weightDenominator) {
    seeder.exam(offering, "Final exam", examDate, examTime, 1, weightDenominator);
  }

  private record CapFixture(UUID groupId, UUID semesterId, UUID courseId) {}
}
