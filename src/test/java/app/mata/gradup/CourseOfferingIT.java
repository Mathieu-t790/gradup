package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.TeacherRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JTeacher;
import app.mata.gradup.repository.model.JTeacherAssignment;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.repository.model.JUser;
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

  @Autowired private CourseOfferingRepository courseOfferingRepository;
  @Autowired private TeacherAssignmentRepository teacherAssignmentRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private TrackRepository trackRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private AcademicYearRepository academicYearRepository;
  @Autowired private SemesterRepository semesterRepository;

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
    assertEquals("Pro1", body.getCourse().getReference());
    assertEquals("K1", body.getGroup().getReference());
    assertEquals(1, body.getSemester().getNumber());
    assertEquals("2024-2025", body.getSemester().getAcademicYearLabel());
    assertTrue(body.getTeachers().isEmpty());
    assertTrue(body.getGradingFinalized());
  }

  @Test
  void getCourseOffering_unknownOffering_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.getForEntity(
            "/course-offerings/" + UUID.randomUUID(), Error.class);

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
    loginAs(restTemplate, "teacher@cu.te");

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
    seedExam(offering, "Final exam", LocalDate.of(2025, 1, 15), LocalTime.of(9, 0, 0), 1, 2);

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
        restTemplate.getForEntity(
            "/course-offerings/" + UUID.randomUUID() + "/exams", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void listOfferingExams_studentRole_canList() {
    var offering = seedOffering();
    seedExam(offering, "Final exam", null, null, 1, 1);
    seedUser("student@cu.te", Role.STUDENT);
    loginAs(restTemplate, "student@cu.te");

    ResponseEntity<ExamResponse[]> response =
        restTemplate.getForEntity(
            "/course-offerings/" + offering.getId() + "/exams", ExamResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().length);
    assertNull(response.getBody()[0].getExamTime());
  }

  private JCourseOffering seedOffering() {
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
                  JTrack.builder().code(TrackCode.EL).label("Ecosysteme Logiciel").build());
          JGroup group =
              groupRepository.save(
                  JGroup.builder().reference("K1").cohort(cohort).track(track).build());
          return seeder.offering(seeder.course("Pro1", 5, 1, track), group, semester);
        });
  }

  private JTeacher seedTeacher() {
    return seeder.inTransaction(
        () -> {
          JUser user =
              userRepository.save(
                  JUser.builder()
                      .lastName("Mathieu")
                      .firstName("Tafita")
                      .email("tafita@cu.te")
                      .passwordHash("hashed")
                      .role(Role.TEACHER)
                      .isActive(true)
                      .build());
          return teacherRepository.save(JTeacher.builder().user(user).specialty(null).build());
        });
  }

  private JTeacherAssignment seedAssignment(JTeacher teacher, JCourseOffering offering) {
    return seeder.inTransaction(
        () -> {
          JTeacher managedTeacher = teacherRepository.findById(teacher.getId()).orElseThrow();
          JCourseOffering managedOffering =
              courseOfferingRepository.findById(offering.getId()).orElseThrow();
          return teacherAssignmentRepository.save(
              JTeacherAssignment.builder()
                  .offering(managedOffering)
                  .teacher(managedTeacher)
                  .build());
        });
  }

  private JExam seedExam(
      JCourseOffering offering,
      String label,
      LocalDate examDate,
      LocalTime examTime,
      int weightNumerator,
      int weightDenominator) {
    return seeder.inTransaction(
        () -> {
          JCourseOffering managedOffering =
              courseOfferingRepository.findById(offering.getId()).orElseThrow();
          return examRepository.save(
              JExam.builder()
                  .offering(managedOffering)
                  .label(label)
                  .examDate(examDate)
                  .examTime(examTime)
                  .weightNumerator(weightNumerator)
                  .weightDenominator(weightDenominator)
                  .build());
        });
  }
}