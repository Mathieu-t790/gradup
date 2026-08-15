package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.DiplomaPageResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaResponse;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.DiplomaMapper;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.DiplomaRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.VGraduationEligibilityRepository;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JDiploma;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.repository.model.JVGraduationEligibility;
import app.mata.gradup.service.utils.XlsxRenderer;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class DiplomaService {

  private static final List<String> EXPORT_HEADERS =
      List.of("Rang", "Référence", "Nom", "Prénom", "Moyenne");

  private final DiplomaRepository diplomaRepository;
  private final CohortRepository cohortRepository;
  private final TrackRepository trackRepository;
  private final StudentRepository studentRepository;
  private final VGraduationEligibilityRepository eligibilityRepository;
  private final DiplomaMapper diplomaMapper;

  @Transactional(readOnly = true)
  public DiplomaPageResponse listCohortDiplomas(
      UUID cohortId, app.mata.gradup.endpoint.rest.model.TrackCode trackCode, Pageable pageable) {
    cohort(cohortId);
    JTrack track = trackCode == null ? null : track(trackCode);
    Page<JDiploma> page =
        track == null
            ? diplomaRepository.findByCohortId(cohortId, pageable)
            : diplomaRepository.findByCohortIdAndTrackId(cohortId, track.getId(), pageable);
    return toPageResponse(page);
  }

  @Transactional
  public List<DiplomaResponse> generateCohortDiplomas(
      UUID cohortId, app.mata.gradup.endpoint.rest.model.TrackCode trackCode) {
    JCohort cohort = cohort(cohortId);
    JTrack track = track(trackCode);

    List<JVGraduationEligibility> eligible =
        eligibilityRepository.findByCohortIdAndTrackIdAndIsEligibleTrue(cohortId, track.getId());
    List<JVGraduationEligibility> ranked = rank(eligible);
    Map<UUID, Integer> rankByStudent = ranksOf(ranked);

    Map<UUID, JDiploma> existingByStudent =
        diplomaRepository.findByCohortIdAndTrackId(cohortId, track.getId()).stream()
            .collect(
                Collectors.toMap(diploma -> diploma.getStudent().getId(), Function.identity()));

    for (JVGraduationEligibility eligibility : ranked) {
      JDiploma existing = existingByStudent.get(eligibility.getStudentId());
      if (existing == null) {
        JStudent student =
            studentRepository
                .findById(eligibility.getStudentId())
                .orElseThrow(
                    () ->
                        new NotFoundException("Student not found: " + eligibility.getStudentId()));
        diplomaRepository.save(
            JDiploma.builder()
                .student(student)
                .cohort(cohort)
                .track(track)
                .overallAverage(eligibility.getOverallAverage())
                .rank(rankByStudent.get(eligibility.getStudentId()))
                .build());
      } else {
        existing.setOverallAverage(eligibility.getOverallAverage());
        existing.setRank(rankByStudent.get(eligibility.getStudentId()));
      }
    }

    List<UUID> eligibleStudentIds =
        ranked.stream().map(JVGraduationEligibility::getStudentId).toList();
    existingByStudent.forEach(
        (studentId, diploma) -> {
          if (!eligibleStudentIds.contains(studentId)) {
            diplomaRepository.delete(diploma);
          }
        });

    return diplomaRepository.findByCohortIdAndTrackId(cohortId, track.getId()).stream()
        .sorted(byRankThenReference())
        .map(diplomaMapper::toRest)
        .toList();
  }

  @Transactional(readOnly = true)
  public ExportResult exportCohortDiplomas(
      UUID cohortId, app.mata.gradup.endpoint.rest.model.TrackCode trackCode) {
    JCohort cohort = cohort(cohortId);
    JTrack track = trackCode == null ? null : track(trackCode);
    List<JDiploma> diplomas =
        track == null
            ? diplomaRepository.findByCohortId(cohortId)
            : diplomaRepository.findByCohortIdAndTrackId(cohortId, track.getId());
    List<List<String>> rows =
        diplomas.stream()
            .sorted(byRankThenReference())
            .map(
                diploma ->
                    List.of(
                        String.valueOf(diploma.getRank()),
                        diploma.getStudent().getUser().getReference(),
                        diploma.getStudent().getUser().getLastName(),
                        diploma.getStudent().getUser().getFirstName(),
                        formattedAverage(diploma.getOverallAverage())))
            .toList();
    byte[] content = XlsxRenderer.render("Diplômes", EXPORT_HEADERS, rows);
    String filename =
        trackCode == null
            ? "diplômés_" + cohort.getLabel() + ".xlsx"
            : "diplômés_" + cohort.getLabel() + "_" + trackCode + ".xlsx";
    return new ExportResult(content, filename);
  }

  private JCohort cohort(UUID cohortId) {
    return cohortRepository
        .findById(cohortId)
        .orElseThrow(() -> new NotFoundException("Cohort not found: " + cohortId));
  }

  private JTrack track(app.mata.gradup.endpoint.rest.model.TrackCode trackCode) {
    app.mata.gradup.model.TrackCode code =
        app.mata.gradup.model.TrackCode.valueOf(trackCode.name());
    return trackRepository
        .findByCode(code)
        .orElseThrow(() -> new NotFoundException("Track not found: " + trackCode));
  }

  private DiplomaPageResponse toPageResponse(Page<JDiploma> page) {
    return new DiplomaPageResponse()
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .first(page.isFirst())
        .last(page.isLast())
        .content(page.getContent().stream().map(diplomaMapper::toRest).toList());
  }

  /** Competition ranking: equal averages share a rank, the next rank skips (1, 2, 2, 4). */
  private static List<JVGraduationEligibility> rank(List<JVGraduationEligibility> eligible) {
    return eligible.stream()
        .sorted(
            Comparator.comparing(
                    JVGraduationEligibility::getOverallAverage,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(eligibility -> eligibility.getStudentId().toString()))
        .toList();
  }

  private static Map<UUID, Integer> ranksOf(List<JVGraduationEligibility> ranked) {
    Map<UUID, Integer> ranks = new HashMap<>();
    int rank = 0;
    BigDecimal previousAverage = null;
    for (int i = 0; i < ranked.size(); i++) {
      JVGraduationEligibility eligibility = ranked.get(i);
      if (previousAverage == null
          || !equalsAverage(previousAverage, eligibility.getOverallAverage())) {
        rank = i + 1;
      }
      ranks.put(eligibility.getStudentId(), rank);
      previousAverage = eligibility.getOverallAverage();
    }
    return ranks;
  }

  private static boolean equalsAverage(BigDecimal a, BigDecimal b) {
    if (a == null && b == null) {
      return true;
    }
    return a != null && b != null && a.compareTo(b) == 0;
  }

  private static Comparator<JDiploma> byRankThenReference() {
    return Comparator.comparingInt(JDiploma::getRank)
        .thenComparing(diploma -> diploma.getStudent().getUser().getReference());
  }

  private static String formattedAverage(BigDecimal average) {
    return average == null ? "" : average.toPlainString();
  }

  public record ExportResult(byte[] content, String filename) {}
}
