package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.TeacherCreateRequest;
import app.mata.gradup.endpoint.rest.model.TeacherResponse;
import app.mata.gradup.mail.Email;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.AdminRepository;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.DiplomaRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GradeDisputeRepository;
import app.mata.gradup.repository.GradeHistoryRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.SemesterCreditValidationRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.TeacherRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.TranscriptDetailRepository;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JTeacher;
import app.mata.gradup.repository.model.JTeacherAssignment;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.repository.model.JUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class TeacherIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private TeacherAssignmentRepository teacherAssignmentRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private TrackRepository trackRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private AcademicYearRepository academicYearRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseOfferingRepository courseOfferingRepository;
  @Autowired private DiplomaRepository diplomaRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeDisputeRepository gradeDisputeRepository;
  @Autowired private GradeHistoryRepository gradeHistoryRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private SemesterCreditValidationRepository semesterCreditValidationRepository;
  @Autowired private StudentGroupHistoryRepository studentGroupHistoryRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private StudentTrackHistoryRepository studentTrackHistoryRepository;
  @Autowired private TranscriptDetailRepository transcriptDetailRepository;
  @Autowired private TranscriptRepository transcriptRepository;
  @Autowired private AdminRepository adminRepository;
  @Autowired private PlatformTransactionManager transactionManager;

  @MockBean private Mailer mailer;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @AfterEach
  void tearDown() {
    cleanDatabase();
  }

  private void cleanDatabase() {
    gradeDisputeRepository.deleteAll();
    gradeHistoryRepository.deleteAll();
    gradeRepository.deleteAll();
    examRepository.deleteAll();
    transcriptDetailRepository.deleteAll();
    transcriptRepository.deleteAll();
    diplomaRepository.deleteAll();
    semesterCreditValidationRepository.deleteAll();
    studentGroupHistoryRepository.deleteAll();
    studentTrackHistoryRepository.deleteAll();
    teacherAssignmentRepository.deleteAll();
    studentRepository.deleteAll();
    courseOfferingRepository.deleteAll();
    courseRepository.deleteAll();
    teacherRepository.deleteAll();
    adminRepository.deleteAll();
    groupRepository.deleteAll();
    semesterRepository.deleteAll();
    academicYearRepository.deleteAll();
    cohortRepository.deleteAll();
    trackRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void listTeachers_returnsEmptyList_whenNone() {
    var response = restTemplate.getForEntity("/teachers", TeacherResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().length);
  }

  @Test
  void listTeachers_returnsAllTeachers() {
    saveTeacher("tafita@cu.te", "Mathematiques");
    saveTeacher("rindra@cu.te", "Programmation");

    var response = restTemplate.getForEntity("/teachers", TeacherResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var teachers = List.of(response.getBody());
    assertEquals(2, teachers.size());
    assertTrue(teachers.stream().anyMatch(t -> t.getEmail().equals("tafita@cu.te")));
    assertTrue(teachers.stream().anyMatch(t -> t.getEmail().equals("rindra@cu.te")));
  }

  @Test
  void createTeacher_returnsCreatedTeacherWithServerGeneratedFields() {
    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te")
                .specialty("Mathematiques"),
            TeacherResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var teacher = response.getBody();
    assertNotNull(teacher);
    assertNotNull(teacher.getId());
    assertEquals("Mathieu", teacher.getLastName());
    assertEquals("Tafita", teacher.getFirstName());
    assertEquals("tafita@cu.te", teacher.getEmail());
    assertNotNull(teacher.getReference());
    assertTrue(teacher.getReference().startsWith("TCH"));
    assertEquals("Mathematiques", teacher.getSpecialty());
    assertTrue(teacherRepository.existsById(teacher.getId()));

    var savedUser = userRepository.findByEmail("tafita@cu.te").orElseThrow();
    assertEquals(Role.TEACHER, savedUser.getRole());
    assertEquals(teacher.getReference(), savedUser.getReference());
  }

  @Test
  void createTeacher_optionalSpecialtyCanBeNull() {
    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te"),
            TeacherResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var teacher = response.getBody();
    assertNotNull(teacher);
    assertNull(teacher.getSpecialty());
  }

  @Test
  void createTeacher_sendsCredentialsEmail() {
    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te"),
            TeacherResponse.class);

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    var email = emailCaptor.getValue();
    var teacher = response.getBody();

    assertEquals("tafita@cu.te", email.to().getAddress());
    assertNotNull(teacher);
    assertTrue(email.subject().contains(teacher.getReference()));
    assertTrue(email.htmlBody().contains("Tafita"));
    assertTrue(email.htmlBody().contains("tafita@cu.te"));
  }

  @Test
  void createTeacher_duplicateEmail_returnsConflict() {
    saveTeacher("taken@cu.te", "Mathematiques");

    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest().lastName("Mathieu").firstName("Tafita").email("taken@cu.te"),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void createTeacher_blankLastName_returnsBadRequest() {
    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest().lastName(" ").firstName("Tafita").email("tafita@cu.te"),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void createTeacher_teacherRole_cannotCreate() {
    seedUser("teacher@cu.te", Role.TEACHER);
    loginAs(restTemplate, "teacher@cu.te");

    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("another@cu.te"),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void listTeacherCourseOfferings_returnsAssignedOfferings() {
    var teacher = saveTeacher("tafita@cu.te", "Mathematiques");
    var offering = saveCourseOffering();
    assignTeacher(teacher, offering);

    var response =
        restTemplate.getForEntity(
            "/teachers/" + teacher.getId() + "/course-offerings", CourseOfferingResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().length);
    var courseOffering = response.getBody()[0];
    assertEquals(offering.getId(), courseOffering.getId());
    assertEquals("Pro1", courseOffering.getCourse().getReference());
    assertEquals("K1", courseOffering.getGroup().getReference());
    assertEquals(1, courseOffering.getSemester().getNumber());
    assertEquals("2024-2025", courseOffering.getSemester().getAcademicYearLabel());
    assertEquals(1, courseOffering.getTeachers().size());
    assertEquals("Tafita", courseOffering.getTeachers().get(0).getFirstName());
    assertFalse(courseOffering.getGradingFinalized());
  }

  @Test
  void listTeacherCourseOfferings_unknownTeacher_returnsNotFound() {
    var response =
        restTemplate.getForEntity(
            "/teachers/" + UUID.randomUUID() + "/course-offerings", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  private JTeacher saveTeacher(String email, String specialty) {
    return inTransaction(
        () -> {
          var user =
              userRepository.save(
                  JUser.builder()
                      .lastName("Mathieu")
                      .firstName("Tafita")
                      .email(email)
                      .passwordHash("hashed")
                      .role(Role.TEACHER)
                      .isActive(true)
                      .build());
          return teacherRepository.save(JTeacher.builder().user(user).specialty(specialty).build());
        });
  }

  private void assignTeacher(JTeacher teacher, JCourseOffering offering) {
    inTransaction(
        () -> {
          var managedTeacher = teacherRepository.findById(teacher.getId()).orElseThrow();
          var managedOffering = courseOfferingRepository.findById(offering.getId()).orElseThrow();
          teacherAssignmentRepository.save(
              JTeacherAssignment.builder()
                  .offering(managedOffering)
                  .teacher(managedTeacher)
                  .build());
          return null;
        });
  }

  private JCourseOffering saveCourseOffering() {
    return inTransaction(
        () -> {
          var academicYear =
              academicYearRepository.save(
                  JAcademicYear.builder()
                      .label("2024-2025")
                      .startDate(LocalDate.of(2024, 9, 1))
                      .endDate(LocalDate.of(2025, 8, 31))
                      .build());
          var semester =
              semesterRepository.save(
                  JSemester.builder()
                      .number(1)
                      .academicYear(academicYear)
                      .startDate(LocalDate.of(2024, 9, 1))
                      .endDate(LocalDate.of(2025, 1, 31))
                      .build());
          var cohort =
              cohortRepository.save(
                  JCohort.builder()
                      .label("Mpamakilay")
                      .entryYear(2021)
                      .expectedGraduationYear(2024)
                      .build());
          var track =
              trackRepository.save(
                  JTrack.builder()
                      .code(app.mata.gradup.model.TrackCode.EL)
                      .label("Ecosysteme Logiciel")
                      .build());
          var group =
              groupRepository.save(
                  JGroup.builder().reference("K1").cohort(cohort).track(track).build());
          var course =
              courseRepository.save(
                  JCourse.builder()
                      .reference("Pro1")
                      .title("Programmation")
                      .credits(5)
                      .semesterNumber(1)
                      .track(track)
                      .build());
          return courseOfferingRepository.save(
              JCourseOffering.builder()
                  .course(course)
                  .group(group)
                  .semester(semester)
                  .gradingFinalized(false)
                  .build());
        });
  }

  private <T> T inTransaction(java.util.function.Supplier<T> action) {
    return new TransactionTemplate(transactionManager).execute(status -> action.get());
  }
}
