package app.mata.gradup.service.utils;

import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JDiploma;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.CourseLine;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.ResultInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TranscriptScoring {

  private static final BigDecimal TEN = BigDecimal.TEN;

  private TranscriptScoring() {}

  public static ResultInfo score(
      List<JCourseOffering> offerings, Map<UUID, BigDecimal> averageByOffering) {
    int totalCredits = 0;
    int creditsAcquired = 0;
    int gradedCredits = 0;
    BigDecimal weightedSum = BigDecimal.ZERO;
    for (JCourseOffering offering : offerings) {
      int credits = offering.getCourse().getCredits();
      BigDecimal average = averageByOffering.get(offering.getId());
      totalCredits += credits;
      if (average == null) {
        continue;
      }
      gradedCredits += credits;
      weightedSum = weightedSum.add(average.multiply(BigDecimal.valueOf(credits)));
      if (hasPassed(average)) {
        creditsAcquired += credits;
      }
    }
    BigDecimal weightedAverage =
        gradedCredits == 0
            ? null
            : weightedSum.divide(BigDecimal.valueOf(gradedCredits), 2, RoundingMode.HALF_UP);
    return new ResultInfo(creditsAcquired, totalCredits, weightedAverage);
  }

  public static boolean hasPassed(BigDecimal average) {
    return average != null && average.compareTo(TEN) >= 0;
  }

  public static List<CourseLine> courseLines(
      List<JCourseOffering> offerings, Map<UUID, BigDecimal> averageByOffering) {
    return offerings.stream()
        .sorted(
            Comparator.comparingInt(
                    (JCourseOffering offering) -> offering.getSemester().getNumber())
                .thenComparing(offering -> offering.getCourse().getReference()))
        .map(
            offering ->
                new CourseLine(
                    offering.getCourse().getReference(),
                    offering.getCourse().getTitle(),
                    offering.getCourse().getCredits(),
                    averageByOffering.get(offering.getId())))
        .toList();
  }

  public static String title(JStudent student, JAcademicYear year) {
    int level = level(student, year);
    return level >= 1 && level <= 3
        ? Wording.get("transcript.title.level", level)
        : Wording.get("transcript.title");
  }

  public static String inscriptionLine(JStudent student, JAcademicYear year, JTrack track) {
    String trackLabel = track == null ? "" : track.getLabel();
    return Wording.get(
        "transcript.inscription",
        levelName(student, year),
        trackLabel.isEmpty() ? "" : " - " + trackLabel,
        year.getLabel());
  }

  public static String diplomaInscriptionLine(JDiploma diploma) {
    return Wording.get(
        "transcript.diploma.inscription",
        diploma.getCohort().getLabel(),
        diploma.getRank(),
        diploma.getTrack().getLabel());
  }

  private static int level(JStudent student, JAcademicYear year) {
    return year.getStartDate().getYear() - student.getCohort().getEntryYear() + 1;
  }

  private static String levelName(JStudent student, JAcademicYear year) {
    int level = level(student, year);
    return level >= 1 && level <= 3
        ? Wording.get("transcript.level." + level)
        : Wording.get("transcript.level.n", level);
  }
}
