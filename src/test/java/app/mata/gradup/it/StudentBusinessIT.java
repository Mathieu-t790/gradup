package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.GradePageResponse;
import app.mata.gradup.endpoint.rest.model.GradeResponse;
import app.mata.gradup.endpoint.rest.model.GraduationEligibilityResponse;
import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentPageResponse;
import app.mata.gradup.endpoint.rest.model.StudentSummaryResponse;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryResponse;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTrack;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

class StudentBusinessIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @Autowired private StudentGroupHistoryRepository groupHistoryRepository;
  @Autowired private StudentTrackHistoryRepository trackHistoryRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void get_list_returns_filtered_page_with_current_group_and_track() {
    Fixture fixture = seed();
    var group = fixture.groupEl;
    var student = fixture.std01;

    var response =
        restTemplate.getForEntity(
            "/students?cohortId=" + fixture.cohort.getId() + "&groupId=" + group.getId(),
            StudentPageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var page = response.getBody();
    assertNotNull(page);
    assertEquals(2, page.getContent().size());
    assertEquals(2L, page.getTotalElements());
    assertTrue(
        page.getContent().stream().noneMatch(item -> item.getId().equals(fixture.std03.getId())));
    StudentSummaryResponse summary =
        page.getContent().stream()
            .filter(item -> item.getId().equals(student.getId()))
            .findFirst()
            .orElseThrow();
    assertEquals("Rakoto", summary.getLastName());
    assertEquals("Hery", summary.getFirstName());
    assertEquals("STD21001", summary.getReference());
    assertEquals(fixture.cohort.getLabel(), summary.getCohortLabel());
    assertEquals(group.getReference(), summary.getCurrentGroupReference());
    assertNotNull(summary.getCurrentTrackCode());
    assertEquals("EL", summary.getCurrentTrackCode().toString());
  }

  @Test
  void get_list_without_filters_returns_all_students() {
    seed();

    var response = restTemplate.getForEntity("/students?page=0&size=10", StudentPageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var page = response.getBody();
    assertNotNull(page);
    assertEquals(3, page.getContent().size());
    assertEquals(3L, page.getTotalElements());
  }

  @Test
  void post_group_history_closes_previous_history_and_creates_new_one() {
    Fixture fixture = seed();
    var student = fixture.std01;
    var group2 = seeder.group("k3", fixture.cohort, fixture.trackEl);

    var response =
        restTemplate.postForEntity(
            "/students/" + student.getId() + "/group-history",
            new StudentGroupHistoryCreateRequest()
                .groupId(group2.getId())
                .startDate(LocalDate.of(2026, 9, 1)),
            StudentGroupHistoryResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var created = response.getBody();
    assertNotNull(created);
    assertEquals(group2.getId(), created.getGroup().getId());
    assertEquals(LocalDate.of(2026, 9, 1), created.getStartDate());

    var histories = groupHistoryRepository.findByStudentIdOrderByStartDateDesc(student.getId());
    assertEquals(2, histories.size());
    assertEquals(LocalDate.of(2026, 8, 31), histories.get(1).getEndDate());
  }

  @Test
  void post_group_history_group_outside_cohort_returns_unprocessable() {
    Fixture fixture = seed();
    var otherCohort = seeder.cohort("Tohindia", 2022, 2025);
    var outsideGroup = seeder.group("z1", otherCohort, fixture.trackEl);

    var response =
        restTemplate.postForEntity(
            "/students/" + fixture.std01.getId() + "/group-history",
            new StudentGroupHistoryCreateRequest()
                .groupId(outsideGroup.getId())
                .startDate(LocalDate.of(2026, 9, 1)),
            Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
  }

  @Test
  void post_track_history_closes_previous_history_and_creates_new_one() {
    Fixture fixture = seed();
    var student = fixture.std01;

    var response =
        restTemplate.postForEntity(
            "/students/" + student.getId() + "/track-history",
            new StudentTrackHistoryCreateRequest()
                .trackId(fixture.trackTn.getId())
                .startDate(LocalDate.of(2026, 9, 1)),
            StudentTrackHistoryResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var created = response.getBody();
    assertNotNull(created);
    assertEquals(fixture.trackTn.getId(), created.getTrack().getId());
    assertEquals(LocalDate.of(2026, 9, 1), created.getStartDate());

    var histories = trackHistoryRepository.findByStudentIdOrderByStartDateDesc(student.getId());
    assertEquals(2, histories.size());
    assertEquals(LocalDate.of(2026, 8, 31), histories.get(1).getEndDate());
  }

  @Test
  void post_group_history_unknown_group_returns_not_found() {
    Fixture fixture = seed();

    var response =
        restTemplate.postForEntity(
            "/students/" + fixture.std01.getId() + "/group-history",
            new StudentGroupHistoryCreateRequest()
                .groupId(UUID.randomUUID())
                .startDate(LocalDate.of(2026, 9, 1)),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void post_group_history_unknown_student_returns_not_found() {
    seed();

    var response =
        restTemplate.postForEntity(
            "/students/" + UUID.randomUUID() + "/group-history",
            new StudentGroupHistoryCreateRequest()
                .groupId(UUID.randomUUID())
                .startDate(LocalDate.of(2026, 9, 1)),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void post_track_history_unknown_track_returns_not_found() {
    Fixture fixture = seed();

    var response =
        restTemplate.postForEntity(
            "/students/" + fixture.std01.getId() + "/track-history",
            new StudentTrackHistoryCreateRequest()
                .trackId(UUID.randomUUID())
                .startDate(LocalDate.of(2026, 9, 1)),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void post_track_history_unknown_student_returns_not_found() {
    seed();

    var response =
        restTemplate.postForEntity(
            "/students/" + UUID.randomUUID() + "/track-history",
            new StudentTrackHistoryCreateRequest()
                .trackId(UUID.randomUUID())
                .startDate(LocalDate.of(2026, 9, 1)),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void get_grades_returns_grades_of_the_student() {
    Fixture fixture = seed();

    var response =
        restTemplate.getForEntity(
            "/students/" + fixture.std01.getId() + "/grades", GradePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var page = response.getBody();
    assertNotNull(page);
    assertEquals(2, page.getContent().size());
    GradeResponse grade = page.getContent().getFirst();
    assertEquals(fixture.std01.getId(), grade.getStudentId());
    assertEquals("Hery Rakoto", grade.getStudentName());
    assertEquals("PROG1", grade.getCourseReference());
    assertEquals(14.0, grade.getScore());
    assertNotNull(grade.getRecordedByName());
  }

  @Test
  void get_grades_filtered_by_semester_returns_only_grades_of_that_semester() {
    Fixture fixture = seed();

    var response =
        restTemplate.getForEntity(
            "/students/" + fixture.std01.getId() + "/grades?semesterId=" + fixture.semester.getId(),
            GradePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var page = response.getBody();
    assertNotNull(page);
    assertEquals(1, page.getContent().size());
    assertEquals("PROG1", page.getContent().getFirst().getCourseReference());
  }

  @Test
  void get_grades_filtered_by_second_semester_excludes_first_semester_grades() {
    Fixture fixture = seed();

    var response =
        restTemplate.getForEntity(
            "/students/"
                + fixture.std01.getId()
                + "/grades?semesterId="
                + fixture.secondSemester.getId(),
            GradePageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var page = response.getBody();
    assertNotNull(page);
    assertEquals(1, page.getContent().size());
    assertEquals("PROG2", page.getContent().getFirst().getCourseReference());
  }

  @Test
  void get_grades_unknown_student_returns_not_found() {
    var response =
        restTemplate.getForEntity("/students/" + UUID.randomUUID() + "/grades", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void get_graduation_eligibility_returns_eligible_student_without_failing_courses() {
    Fixture fixture = seed();

    var response =
        restTemplate.getForEntity(
            "/students/" + fixture.std01.getId() + "/graduation-eligibility",
            GraduationEligibilityResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var eligibility = response.getBody();
    assertNotNull(eligibility);
    assertEquals(fixture.std01.getId(), eligibility.getStudentId());
    assertEquals("EL", eligibility.getTrack().getCode().toString());
    assertTrue(eligibility.getIsEligible());
    assertEquals(13.0, eligibility.getOverallAverage());
    assertTrue(eligibility.getFailingCourses().isEmpty());
  }

  @Test
  void get_graduation_eligibility_returns_failing_courses_for_student_below_threshold() {
    Fixture fixture = seed();
    seeder.changeScore("STD21002", "PROG1", new BigDecimal("8.00"));

    var response =
        restTemplate.getForEntity(
            "/students/" + fixture.std02.getId() + "/graduation-eligibility",
            GraduationEligibilityResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var eligibility = response.getBody();
    assertNotNull(eligibility);
    assertFalse(eligibility.getIsEligible());
    assertEquals(1, eligibility.getFailingCourses().size());
    assertEquals("PROG1", eligibility.getFailingCourses().getFirst().getCourse().getReference());
    assertEquals(8.0, eligibility.getFailingCourses().getFirst().getAverage());
  }

  @Test
  void get_graduation_eligibility_student_without_grades_is_not_eligible() {
    Fixture fixture = seed();

    var response =
        restTemplate.getForEntity(
            "/students/" + fixture.std03.getId() + "/graduation-eligibility",
            GraduationEligibilityResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var eligibility = response.getBody();
    assertNotNull(eligibility);
    assertEquals(fixture.std03.getId(), eligibility.getStudentId());
    assertEquals("TN", eligibility.getTrack().getCode().toString());
    assertFalse(eligibility.getIsEligible());
    assertTrue(eligibility.getFailingCourses().isEmpty());
  }

  @Test
  void get_graduation_eligibility_unknown_student_returns_not_found() {
    var response =
        restTemplate.getForEntity(
            "/students/" + UUID.randomUUID() + "/graduation-eligibility", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  private Fixture seed() {
    return seeder.inTransaction(
        () -> {
          JCohort cohort = seeder.cohort("Mpamakilay", 2021, 2024);
          JTrack trackEl = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
          JTrack trackTn = seeder.track(TrackCode.TN, "Transformation Numerique");
          JGroup groupEl = seeder.group("k1", cohort, trackEl);
          JGroup groupTn = seeder.group("k2", cohort, trackTn);
          JAcademicYear year =
              seeder.academicYear("2025-2026", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
          JSemester semester =
              seeder.semester(1, year, LocalDate.of(2025, 9, 1), LocalDate.of(2026, 1, 31));
          JSemester secondSemester =
              seeder.semester(2, year, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 31));

          JStudent std01 =
              seeder.student("STD21001", "Rakoto", "Hery", "hery@cu.te", cohort, trackEl, groupEl);
          JStudent std02 =
              seeder.student("STD21002", "Rabe", "Mialy", "mialy@cu.te", cohort, trackEl, groupEl);
          JStudent std03 =
              seeder.student(
                  "STD21003", "Andria", "Tiana", "tiana@cu.te", cohort, trackTn, groupTn);

          var course = seeder.course("PROG1", 6, 1, trackEl);
          var offering = seeder.offering(course, groupEl, semester);
          var exam = seeder.exam(offering);
          seeder.grade(std01, exam, "14.00");
          seeder.grade(std02, exam, "16.00");

          var secondCourse = seeder.course("PROG2", 6, 2, trackEl);
          var secondOffering = seeder.offering(secondCourse, groupEl, secondSemester);
          var secondExam = seeder.exam(secondOffering);
          seeder.grade(std01, secondExam, "12.00");

          return new Fixture(
              cohort,
              trackEl,
              trackTn,
              groupEl,
              groupTn,
              semester,
              secondSemester,
              std01,
              std02,
              std03);
        });
  }

  private record Fixture(
      JCohort cohort,
      JTrack trackEl,
      JTrack trackTn,
      JGroup groupEl,
      JGroup groupTn,
      JSemester semester,
      JSemester secondSemester,
      JStudent std01,
      JStudent std02,
      JStudent std03) {}
}
