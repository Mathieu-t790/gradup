package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.CourseOfferingCreateRequest;
import app.mata.gradup.endpoint.rest.model.CourseOfferingPageResponse;
import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.ExamCreateRequest;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.endpoint.rest.model.ExamUpdateRequest;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.TeacherRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JTeacher;
import app.mata.gradup.repository.model.JTeacherAssignment;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.repository.model.JUser;
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

class CourseOfferingExamIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @Autowired private CourseOfferingRepository courseOfferingRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private TeacherAssignmentRepository teacherAssignmentRepository;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void listCourseOfferings_returnsPagedOfferings() {
    Seed seed = seed();
    seedOffering(seed.course("Pro1"), seed.group, seed.semester, true);
    seedOffering(seed.course("Pro2"), seed.group, seed.semester, false);

    ResponseEntity<CourseOfferingPageResponse> response =
        restTemplate.getForEntity(
            "/course-offerings?page=0&size=1", CourseOfferingPageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    CourseOfferingPageResponse body = response.getBody();
    assertNotNull(body);
    assertEquals(0, body.getPage());
    assertEquals(1, body.getSize());
    assertEquals(2L, body.getTotalElements());
    assertEquals(2, body.getTotalPages());
    assertEquals(1, body.getContent().size());
  }

  @Test
  void listCourseOfferings_filtersBySemesterGroupAndCourse() {
    Seed seed = seed();
    var offering1 = seedOffering(seed.course("Pro1"), seed.group, seed.semester, true);
    var offering2 =
        seedOffering(
            seed.course("Pro2"), seed.group(seed.cohort, "K2"), seed.semester(seed.year), true);

    ResponseEntity<CourseOfferingPageResponse> semesterFilter =
        restTemplate.getForEntity(
            "/course-offerings?semesterId=" + seed.semester.getId(),
            CourseOfferingPageResponse.class);
    assertNotNull(semesterFilter.getBody());
    assertEquals(1L, semesterFilter.getBody().getTotalElements());

    ResponseEntity<CourseOfferingPageResponse> groupFilter =
        restTemplate.getForEntity(
            "/course-offerings?groupId=" + offering2.getGroup().getId(),
            CourseOfferingPageResponse.class);
    assertNotNull(groupFilter.getBody());
    assertEquals(1L, groupFilter.getBody().getTotalElements());
    assertEquals("Pro2", groupFilter.getBody().getContent().getFirst().getCourse().getReference());

    ResponseEntity<CourseOfferingPageResponse> courseFilter =
        restTemplate.getForEntity(
            "/course-offerings?courseId=" + offering1.getCourse().getId(),
            CourseOfferingPageResponse.class);
    assertNotNull(courseFilter.getBody());
    assertEquals(1L, courseFilter.getBody().getTotalElements());

    ResponseEntity<CourseOfferingPageResponse> noFilter =
        restTemplate.getForEntity("/course-offerings", CourseOfferingPageResponse.class);
    assertNotNull(noFilter.getBody());
    assertEquals(2L, noFilter.getBody().getTotalElements());
  }

  @Test
  void listCourseOfferings_returnsTeachersPerOffering() {
    Seed seed = seed();
    var offering = seedOffering(seed.course("Pro1"), seed.group, seed.semester, true);
    seedTeacher(offering);

    ResponseEntity<CourseOfferingPageResponse> response =
        restTemplate.getForEntity("/course-offerings", CourseOfferingPageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var content = response.getBody().getContent().getFirst();
    assertTrue(content.getTeachers().stream().anyMatch(t -> t.getLastName().equals("Mathieu")));
  }

  @Test
  void createCourseOffering_returnsCreatedOffering() {
    Seed seed = seed();
    var course = seed.course("Pro1");

    ResponseEntity<CourseOfferingResponse> response =
        restTemplate.postForEntity(
            "/course-offerings",
            new CourseOfferingCreateRequest()
                .courseId(course.getId())
                .groupId(seed.group.getId())
                .semesterId(seed.semester.getId()),
            CourseOfferingResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    CourseOfferingResponse body = response.getBody();
    assertNotNull(body);
    assertNotNull(body.getId());
    assertEquals("Pro1", body.getCourse().getReference());
    assertEquals("K1", body.getGroup().getReference());
    assertEquals(1, body.getSemester().getNumber());
    assertFalse(body.getGradingFinalized());
    assertTrue(body.getTeachers().isEmpty());
  }

  @Test
  void createCourseOffering_duplicate_returnsConflict() {
    Seed seed = seed();
    var course = seed.course("Pro1");
    seedOffering(course, seed.group, seed.semester, true);

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/course-offerings",
            new CourseOfferingCreateRequest()
                .courseId(course.getId())
                .groupId(seed.group.getId())
                .semesterId(seed.semester.getId()),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void createCourseOffering_semesterMismatch_returnsUnprocessableEntity() {
    Seed seed = seed();
    var course = seed.course("Pro1", 5, 2);

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/course-offerings",
            new CourseOfferingCreateRequest()
                .courseId(course.getId())
                .groupId(seed.group.getId())
                .semesterId(seed.semester.getId()),
            Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UNPROCESSABLE_ENTITY", response.getBody().getCode());
  }

  @Test
  void createCourseOffering_exceedsSemesterCredits_returnsUnprocessableEntity() {
    Seed seed = seed();
    seedOffering(seed.course("Heavy", 30, 1), seed.group, seed.semester, true);
    var extra = seed.course("Extra", 1, 1);

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/course-offerings",
            new CourseOfferingCreateRequest()
                .courseId(extra.getId())
                .groupId(seed.group.getId())
                .semesterId(seed.semester.getId()),
            Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
  }

  @Test
  void createCourseOffering_exceedsYearCredits_returnsUnprocessableEntity() {
    Seed seed = seed();
    var semester2 = seed.semester(seed.year);
    seedOffering(seed.course("Heavy", 40, 1), seed.group, seed.semester, true);
    seedOffering(seed.course("S2", 25, 2), seed.group, semester2, true);
    var extra = seed.course("Extra", 5, 2);

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/course-offerings",
            new CourseOfferingCreateRequest()
                .courseId(extra.getId())
                .groupId(seed.group.getId())
                .semesterId(semester2.getId()),
            Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
  }

  @Test
  void createCourseOffering_unknownEntities_returnsNotFound() {
    ResponseEntity<Error> courseNotFound =
        restTemplate.postForEntity(
            "/course-offerings",
            new CourseOfferingCreateRequest()
                .courseId(UUID.randomUUID())
                .groupId(UUID.randomUUID())
                .semesterId(UUID.randomUUID()),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, courseNotFound.getStatusCode());

    Seed seed = seed();
    var course = seed.course("Pro1");
    ResponseEntity<Error> groupNotFound =
        restTemplate.postForEntity(
            "/course-offerings",
            new CourseOfferingCreateRequest()
                .courseId(course.getId())
                .groupId(UUID.randomUUID())
                .semesterId(seed.semester.getId()),
            Error.class);
    assertEquals(HttpStatus.NOT_FOUND, groupNotFound.getStatusCode());
  }

  @Test
  void createCourseOffering_commonGroup_skipsCreditRule() {
    Seed seed = seed();
    var commonGroup = seed.group(seed.cohort, null);
    var course = seed.course(null);

    ResponseEntity<CourseOfferingResponse> response =
        restTemplate.postForEntity(
            "/course-offerings",
            new CourseOfferingCreateRequest()
                .courseId(course.getId())
                .groupId(commonGroup.getId())
                .semesterId(seed.semester.getId()),
            CourseOfferingResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }

  @Test
  void createExam_returnsCreatedExam() {
    var offering = seedOfferingFromScratch();

    ResponseEntity<ExamResponse> response =
        restTemplate.postForEntity(
            "/course-offerings/" + offering.getId() + "/exams",
            new ExamCreateRequest()
                .label("Final")
                .examDate(LocalDate.of(2022, 5, 30))
                .examTime("09:30:00")
                .weightNumerator(1)
                .weightDenominator(2),
            ExamResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    ExamResponse body = response.getBody();
    assertNotNull(body);
    assertNotNull(body.getId());
    assertEquals(offering.getId(), body.getOfferingId());
    assertEquals("Final", body.getLabel());
    assertEquals("09:30:00", body.getExamTime());
    assertEquals(1, body.getWeightNumerator());
    assertEquals(2, body.getWeightDenominator());
  }

  @Test
  void createExam_weightSumExceedsOne_returnsUnprocessableEntity() {
    var offering = seedOfferingFromScratch();
    seedExam(offering, 3, 4);

    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/course-offerings/" + offering.getId() + "/exams",
            new ExamCreateRequest().label("Final").weightNumerator(1).weightDenominator(2),
            Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UNPROCESSABLE_ENTITY", response.getBody().getCode());
  }

  @Test
  void createExam_whenSumReachesOne_finalizesGrading() {
    var offering = seedOfferingFromScratch();
    seedExam(offering, 1, 2);

    ResponseEntity<ExamResponse> response =
        restTemplate.postForEntity(
            "/course-offerings/" + offering.getId() + "/exams",
            new ExamCreateRequest().label("Final").weightNumerator(1).weightDenominator(2),
            ExamResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(
        courseOfferingRepository.findById(offering.getId()).orElseThrow().getGradingFinalized());
  }

  @Test
  void createExam_unknownOffering_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.postForEntity(
            "/course-offerings/" + UUID.randomUUID() + "/exams",
            new ExamCreateRequest().label("Final").weightNumerator(1).weightDenominator(1),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void updateExam_returnsUpdatedExam() {
    var offering = seedOfferingFromScratch();
    var exam = seedExam(offering, 1, 4);
    seedExam(offering, 1, 4);

    ResponseEntity<ExamResponse> response =
        restTemplate.exchange(
            "/exams/" + exam.getId(),
            HttpMethod.PATCH,
            new HttpEntity<>(
                new ExamUpdateRequest()
                    .label("CC1")
                    .examTime("10:00:00")
                    .weightNumerator(1)
                    .weightDenominator(2)),
            ExamResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    ExamResponse body = response.getBody();
    assertNotNull(body);
    assertEquals("CC1", body.getLabel());
    assertEquals("10:00:00", body.getExamTime());
    assertEquals(1, body.getWeightNumerator());
    assertEquals(2, body.getWeightDenominator());
  }

  @Test
  void updateExam_whenSumReachesOne_finalizesGrading() {
    var offering = seedOfferingFromScratch();
    var exam = seedExam(offering, 1, 4);
    seedExam(offering, 1, 4);
    seedExam(offering, 1, 4);

    ResponseEntity<ExamResponse> response =
        restTemplate.exchange(
            "/exams/" + exam.getId(),
            HttpMethod.PATCH,
            new HttpEntity<>(
                new ExamUpdateRequest().label("CC1").weightNumerator(1).weightDenominator(2)),
            ExamResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(
        courseOfferingRepository.findById(offering.getId()).orElseThrow().getGradingFinalized());
  }

  @Test
  void updateExam_weightSumExceedsOne_returnsUnprocessableEntity() {
    var offering = seedOfferingFromScratch();
    var exam = seedExam(offering, 1, 2);
    seedExam(offering, 1, 2);

    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/exams/" + exam.getId(),
            HttpMethod.PATCH,
            new HttpEntity<>(
                new ExamUpdateRequest().label("Final").weightNumerator(1).weightDenominator(1)),
            Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
  }

  @Test
  void updateExam_unknownExam_returnsNotFound() {
    ResponseEntity<Error> response =
        restTemplate.exchange(
            "/exams/" + UUID.randomUUID(),
            HttpMethod.PATCH,
            new HttpEntity<>(
                new ExamUpdateRequest().label("Final").weightNumerator(1).weightDenominator(1)),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void updateExam_partialBody_keepsOtherFields() {
    var offering = seedOfferingFromScratch();
    var exam = seedExam(offering, 1, 2);

    ResponseEntity<ExamResponse> response =
        restTemplate.exchange(
            "/exams/" + exam.getId(),
            HttpMethod.PATCH,
            new HttpEntity<>(new ExamUpdateRequest().label("Renamed")),
            ExamResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    ExamResponse body = response.getBody();
    assertNotNull(body);
    assertEquals("Renamed", body.getLabel());
    assertEquals(LocalDate.of(2022, 5, 30), body.getExamDate());
    assertEquals(1, body.getWeightNumerator());
    assertEquals(2, body.getWeightDenominator());
  }

  private Seed seed() {
    return seeder.inTransaction(
        () -> {
          JCohort cohort = seeder.cohort("Mpamakilay", 2021, 2024);
          JTrack track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
          JGroup group = seeder.group("K1", cohort, track);
          JAcademicYear year =
              seeder.academicYear("2024-2025", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 8, 31));
          JSemester semester =
              seeder.semester(1, year, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 31));
          return new Seed(seeder, cohort, track, group, year, semester);
        });
  }

  private record Seed(
      TestDataSeeder seeder,
      JCohort cohort,
      JTrack track,
      JGroup group,
      JAcademicYear year,
      JSemester semester) {

    private JGroup group(JCohort cohort, String reference) {
      return seeder.group(reference, cohort, track);
    }

    private JSemester semester(JAcademicYear year) {
      return seeder.semester(2, year, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 31));
    }

    private JCourse course(String reference) {
      return seeder.course(reference, 5, 1, track);
    }

    private JCourse course(String reference, int credits, int semesterNumber) {
      return seeder.course(reference, credits, semesterNumber, track);
    }
  }

  private JCourseOffering seedOfferingFromScratch() {
    Seed seed = seed();
    return seedOffering(seed.course("Pro1"), seed.group, seed.semester, false);
  }

  private JCourseOffering seedOffering(
      JCourse course, JGroup group, JSemester semester, boolean gradingFinalized) {
    return seeder.inTransaction(
        () ->
            courseOfferingRepository.save(
                JCourseOffering.builder()
                    .course(course)
                    .group(group)
                    .semester(semester)
                    .gradingFinalized(gradingFinalized)
                    .build()));
  }

  private JExam seedExam(JCourseOffering offering, int numerator, int denominator) {
    return seeder.inTransaction(
        () ->
            examRepository.save(
                JExam.builder()
                    .offering(offering)
                    .label("Final")
                    .examDate(LocalDate.of(2022, 5, 30))
                    .weightNumerator(numerator)
                    .weightDenominator(denominator)
                    .build()));
  }

  private void seedTeacher(JCourseOffering offering) {
    seeder.inTransaction(
        () -> {
          var user =
              userRepository.save(
                  JUser.builder()
                      .lastName("Mathieu")
                      .firstName("Tafita")
                      .email("teacher@cu.te")
                      .passwordHash("hashed")
                      .role(Role.TEACHER)
                      .isActive(true)
                      .build());
          var teacher = teacherRepository.save(JTeacher.builder().user(user).build());
          var managedOffering = courseOfferingRepository.findById(offering.getId()).orElseThrow();
          teacherAssignmentRepository.save(
              JTeacherAssignment.builder().offering(managedOffering).teacher(teacher).build());
          return null;
        });
  }
}
