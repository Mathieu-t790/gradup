package app.mata.gradup.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JDiploma;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.repository.model.JUser;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.CourseLine;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.ResultInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TranscriptScoringTest {

  @Test
  void score_computes_credit_weighted_average_and_acquired_credits() {
    JCourseOffering prog = offering(course("PROG1", 6), 1);
    JCourseOffering lv1 = offering(course("LV1", 4), 1);
    JCourseOffering lv2 = offering(course("LV2", 2), 1);
    Map<UUID, BigDecimal> averages =
        Map.of(
            prog.getId(), new BigDecimal("12.50"),
            lv1.getId(), new BigDecimal("9.50"));

    ResultInfo result = TranscriptScoring.score(List.of(prog, lv1, lv2), averages);

    assertEquals(12, result.totalCredits());
    assertEquals(6, result.creditsAcquired());
    assertEquals(new BigDecimal("11.30"), result.weightedAverage());
  }

  @Test
  void score_without_grades_has_no_average_and_no_acquired_credits() {
    JCourseOffering prog = offering(course("PROG1", 6), 1);
    JCourseOffering lv1 = offering(course("LV1", 4), 1);

    ResultInfo result = TranscriptScoring.score(List.of(prog, lv1), Map.of());

    assertEquals(10, result.totalCredits());
    assertEquals(0, result.creditsAcquired());
    assertNull(result.weightedAverage());
  }

  @Test
  void hasPassed_threshold_is_10() {
    assertTrue(TranscriptScoring.hasPassed(BigDecimal.TEN));
    assertTrue(TranscriptScoring.hasPassed(new BigDecimal("12.50")));
    assertFalse(TranscriptScoring.hasPassed(new BigDecimal("9.99")));
    assertFalse(false);
  }

  @Test
  void courseLines_are_sorted_by_semester_then_course_reference() {
    JCourseOffering lv2 = offering(course("LV2", 4), 2);
    JCourseOffering prog1 = offering(course("PROG1", 4), 1);
    JCourseOffering lv1 = offering(course("LV1", 4), 1);
    JCourseOffering pro1 = offering(course("PRO1", 4), 1);

    List<CourseLine> lines =
        TranscriptScoring.courseLines(
            List.of(lv2, prog1, lv1, pro1),
            Map.of(
                lv2.getId(), BigDecimal.TEN,
                prog1.getId(), BigDecimal.TEN,
                lv1.getId(), BigDecimal.TEN,
                pro1.getId(), BigDecimal.TEN));

    assertEquals(
        List.of("LV1", "PRO1", "PROG1", "LV2"), lines.stream().map(CourseLine::code).toList());
  }

  @Test
  void title_contains_the_computed_level() {
    JStudent student = student();
    assertTrue(TranscriptScoring.title(student, year(2025)).contains("L2"));
    assertFalse(TranscriptScoring.title(student, year(2029)).contains("L"));
  }

  @Test
  void inscriptionLine_includes_track_and_year_label() {
    JStudent student = student();
    JTrack track = JTrack.builder().code(TrackCode.EL).label("Ecosysteme Logiciel").build();

    String withTrack = TranscriptScoring.inscriptionLine(student, year(2025), track);
    String withoutTrack = TranscriptScoring.inscriptionLine(student, year(2025), null);

    assertTrue(withTrack.contains("Ecosysteme Logiciel"));
    assertTrue(withTrack.contains("2025-2026"));
    assertTrue(withoutTrack.contains("2025-2026"));
    assertFalse(withoutTrack.contains("Ecosysteme Logiciel"));
  }

  @Test
  void diplomaInscriptionLine_includes_promotion_rank_and_track() {
    JDiploma diploma =
        JDiploma.builder()
            .cohort(JCohort.builder().label("Promo 2024").build())
            .track(JTrack.builder().label("Transformation Numerique").build())
            .rank(3)
            .build();

    String line = TranscriptScoring.diplomaInscriptionLine(diploma);

    assertTrue(line.contains("Promo 2024"));
    assertTrue(line.contains("3e"));
    assertTrue(line.contains("Transformation Numerique"));
  }

  private JCourseOffering offering(JCourse course, int semesterNumber) {
    JSemester semester = JSemester.builder().number(semesterNumber).build();
    return JCourseOffering.builder()
        .id(UUID.randomUUID())
        .course(course)
        .semester(semester)
        .build();
  }

  private JCourse course(String reference, int credits) {
    return JCourse.builder()
        .reference(reference)
        .title("Course")
        .credits(credits)
        .semesterNumber(1)
        .build();
  }

  private JStudent student() {
    return JStudent.builder()
        .user(JUser.builder().reference("STD" + 2024 + "001").build())
        .cohort(JCohort.builder().label("Promo " + 2024).entryYear(2024).build())
        .build();
  }

  private JAcademicYear year(int startYear) {
    return JAcademicYear.builder()
        .label(startYear + "-" + (startYear + 1))
        .startDate(LocalDate.of(startYear, 9, 1))
        .endDate(LocalDate.of(startYear + 1, 7, 31))
        .build();
  }
}
