package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.DiplomaExportResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaPageResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaResponse;
import app.mata.gradup.endpoint.rest.model.TrackCode;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.file.bucket.BucketComponent;
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
import app.mata.gradup.service.utils.BucketExporter;
import app.mata.gradup.service.utils.Pages;
import app.mata.gradup.service.utils.Ranking;
import app.mata.gradup.service.utils.TrackCodes;
import app.mata.gradup.service.utils.Wording;
import app.mata.gradup.service.utils.XlsxRenderer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
      List.of(
          Wording.get("diploma.export.header.rank"),
          Wording.get("diploma.export.header.reference"),
          Wording.get("diploma.export.header.lastName"),
          Wording.get("diploma.export.header.firstName"),
          Wording.get("diploma.export.header.average"));

  private static final String EXPORT_BUCKET_PREFIX = "diplomas";

  private final DiplomaRepository diplomaRepository;
  private final CohortRepository cohortRepository;
  private final TrackRepository trackRepository;
  private final StudentRepository studentRepository;
  private final VGraduationEligibilityRepository eligibilityRepository;
  private final DiplomaMapper diplomaMapper;
  private final BucketComponent bucketComponent;

  @Transactional(readOnly = true)
  public DiplomaPageResponse listCohortDiplomas(
      UUID cohortId, TrackCode trackCode, Pageable pageable) {
    cohort(cohortId);
    if (trackCode == null) {
      return promotionList(cohortId, pageable);
    }
    JTrack track = track(trackCode);
    Page<JDiploma> page =
        diplomaRepository.findByCohortIdAndTrackId(cohortId, track.getId(), pageable);
    return toPageResponse(page.map(diplomaMapper::toRest));
  }

  @Transactional
  public List<DiplomaResponse> generateCohortDiplomas(UUID cohortId, TrackCode trackCode) {
    JCohort cohort = cohort(cohortId);
    if (trackCode == null) {
      return generateForPromotion(cohort);
    }
    return generateForTrack(cohort, track(trackCode));
  }

  private List<DiplomaResponse> generateForTrack(JCohort cohort, JTrack track) {
    return persistForTrack(
            cohort,
            track,
            eligibilityRepository.findByCohortIdAndTrackIdAndIsEligibleTrue(
                cohort.getId(), track.getId()))
        .stream()
        .sorted(byRankThenReference())
        .map(diplomaMapper::toRest)
        .toList();
  }

  private List<DiplomaResponse> generateForPromotion(JCohort cohort) {
    UUID cohortId = cohort.getId();
    List<JVGraduationEligibility> eligible =
        eligibilityRepository.findByCohortIdAndIsEligibleTrue(cohortId);
    List<JVGraduationEligibility> promotionRanked =
        Ranking.sortByAverageDesc(
            eligible,
            JVGraduationEligibility::getOverallAverage,
            JVGraduationEligibility::getStudentId);
    Map<UUID, Integer> promotionRanks =
        Ranking.competitionRanks(
            promotionRanked,
            JVGraduationEligibility::getOverallAverage,
            JVGraduationEligibility::getStudentId);

    persistEachTrack(cohort, eligible);

    Map<UUID, JDiploma> diplomasByStudent =
        diplomaRepository.findByCohortId(cohortId).stream()
            .collect(Collectors.toMap(d -> d.getStudent().getId(), Function.identity()));
    return promotionRanked.stream()
        .map(
            eligibility ->
                toPromotionRankedResponse(
                    diplomasByStudent.get(eligibility.getStudentId()), promotionRanks))
        .sorted(
            Comparator.comparingInt(DiplomaResponse::getRank)
                .thenComparing(diploma -> diploma.getStudent().getReference()))
        .toList();
  }

  private void persistEachTrack(JCohort cohort, List<JVGraduationEligibility> eligible) {
    Map<UUID, List<JVGraduationEligibility>> byTrack =
        eligible.stream().collect(Collectors.groupingBy(JVGraduationEligibility::getTrackId));
    for (Map.Entry<UUID, List<JVGraduationEligibility>> entry : byTrack.entrySet()) {
      JTrack track =
          trackRepository
              .findById(entry.getKey())
              .orElseThrow(() -> new NotFoundException("Track not found: " + entry.getKey()));
      persistForTrack(cohort, track, entry.getValue());
    }
  }

  private List<JDiploma> persistForTrack(
      JCohort cohort, JTrack track, List<JVGraduationEligibility> eligible) {
    UUID cohortId = cohort.getId();
    List<JVGraduationEligibility> ranked =
        Ranking.sortByAverageDesc(
            eligible,
            JVGraduationEligibility::getOverallAverage,
            JVGraduationEligibility::getStudentId);
    Map<UUID, Integer> rankByStudent =
        Ranking.competitionRanks(
            ranked,
            JVGraduationEligibility::getOverallAverage,
            JVGraduationEligibility::getStudentId);

    Map<UUID, JDiploma> existingByStudent =
        diplomaRepository.findByCohortIdAndTrackId(cohortId, track.getId()).stream()
            .collect(
                Collectors.toMap(diploma -> diploma.getStudent().getId(), Function.identity()));

    Map<UUID, JStudent> studentsByStudentId =
        ranked.stream()
            .map(JVGraduationEligibility::getStudentId)
            .filter(studentId -> !existingByStudent.containsKey(studentId))
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    studentIds ->
                        studentIds.isEmpty()
                            ? Map.of()
                            : studentRepository.findAllById(studentIds).stream()
                                .collect(Collectors.toMap(JStudent::getId, Function.identity()))));

    for (JVGraduationEligibility eligibility : ranked) {
      JDiploma existing = existingByStudent.get(eligibility.getStudentId());
      if (existing == null) {
        JStudent student = studentsByStudentId.get(eligibility.getStudentId());
        if (student == null) {
          throw new NotFoundException("Student not found: " + eligibility.getStudentId());
        }
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

    return diplomaRepository.findByCohortIdAndTrackId(cohortId, track.getId());
  }

  private DiplomaPageResponse promotionList(UUID cohortId, Pageable pageable) {
    List<JDiploma> diplomas = diplomaRepository.findByCohortId(cohortId);
    Map<UUID, Integer> promotionRanks =
        Ranking.competitionRanks(
            Ranking.sortByAverageDesc(
                diplomas, JDiploma::getOverallAverage, diploma -> diploma.getStudent().getId()),
            JDiploma::getOverallAverage,
            diploma -> diploma.getStudent().getId());
    List<DiplomaResponse> ordered =
        diplomas.stream()
            .sorted(byPromotionRankThenReference(promotionRanks))
            .map(diploma -> toPromotionRankedResponse(diploma, promotionRanks))
            .toList();
    return toPageResponse(Pages.subPage(ordered, pageable));
  }

  @Transactional(readOnly = true)
  public DiplomaExportResponse exportCohortDiplomas(UUID cohortId, TrackCode trackCode) {
    JCohort cohort = cohort(cohortId);
    List<JDiploma> diplomas =
        trackCode == null
            ? diplomaRepository.findByCohortId(cohortId)
            : diplomaRepository.findByCohortIdAndTrackId(cohortId, track(trackCode).getId());
    List<List<String>> rows = exportRows(trackCode == null, diplomas);
    byte[] content = XlsxRenderer.render(Wording.get("diploma.export.sheet"), EXPORT_HEADERS, rows);
    String filename = buildExportFilename(trackCode, cohort.getLabel(), LocalDate.now());
    String bucketKey = EXPORT_BUCKET_PREFIX + "/" + cohortId + "/" + filename;
    String downloadUrl = BucketExporter.uploadAndPresign(bucketComponent, content, bucketKey);
    return new DiplomaExportResponse().fileName(bucketKey).downloadUrl(downloadUrl);
  }

  static String buildExportFilename(TrackCode trackCode, String cohortLabel, LocalDate date) {
    String scope = trackCode == null ? "TroncCommun" : trackLabel(trackCode);
    String label = cohortLabel == null ? "" : cohortLabel;
    String day = date == null ? "" : date.format(DateTimeFormatter.BASIC_ISO_DATE);
    return Wording.get("diploma.export.filename.prefix")
        + scope
        + "_"
        + label
        + "_"
        + day
        + ".xlsx";
  }

  private static String trackLabel(TrackCode trackCode) {
    return switch (trackCode) {
      case EL -> "El";
      case TN -> "Tn";
    };
  }

  private List<List<String>> exportRows(boolean promotion, List<JDiploma> diplomas) {
    if (!promotion) {
      return diplomas.stream()
          .sorted(byRankThenReference())
          .map(diploma -> row(String.valueOf(diploma.getRank()), diploma))
          .toList();
    }
    Map<UUID, Integer> promotionRanks =
        Ranking.competitionRanks(
            Ranking.sortByAverageDesc(
                diplomas, JDiploma::getOverallAverage, diploma -> diploma.getStudent().getId()),
            JDiploma::getOverallAverage,
            diploma -> diploma.getStudent().getId());
    return diplomas.stream()
        .sorted(byPromotionRankThenReference(promotionRanks))
        .map(
            diploma ->
                row(String.valueOf(promotionRanks.get(diploma.getStudent().getId())), diploma))
        .toList();
  }

  private static List<String> row(String rank, JDiploma diploma) {
    return List.of(
        rank,
        diploma.getStudent().getUser().getReference(),
        diploma.getStudent().getUser().getLastName(),
        diploma.getStudent().getUser().getFirstName(),
        formattedAverage(diploma.getOverallAverage()));
  }

  private JCohort cohort(UUID cohortId) {
    return cohortRepository
        .findById(cohortId)
        .orElseThrow(() -> new NotFoundException("Cohort not found: " + cohortId));
  }

  private JTrack track(TrackCode trackCode) {
    return trackRepository
        .findByCode(TrackCodes.toDomain(trackCode))
        .orElseThrow(() -> new NotFoundException("Track not found: " + trackCode));
  }

  private DiplomaPageResponse toPageResponse(Page<DiplomaResponse> page) {
    return new DiplomaPageResponse()
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .first(page.isFirst())
        .last(page.isLast())
        .content(page.getContent());
  }

  private static Comparator<JDiploma> byRankThenReference() {
    return Comparator.comparingInt(JDiploma::getRank)
        .thenComparing(diploma -> diploma.getStudent().getUser().getReference());
  }

  private static Comparator<JDiploma> byPromotionRankThenReference(
      Map<UUID, Integer> promotionRanks) {
    return Comparator.comparingInt(
            (JDiploma diploma) -> promotionRanks.get(diploma.getStudent().getId()))
        .thenComparing(diploma -> diploma.getStudent().getUser().getReference());
  }

  private DiplomaResponse toPromotionRankedResponse(
      JDiploma diploma, Map<UUID, Integer> promotionRanks) {
    return diplomaMapper.toRest(diploma).rank(promotionRanks.get(diploma.getStudent().getId()));
  }

  private static String formattedAverage(BigDecimal average) {
    return average == null ? "" : average.toPlainString();
  }
}
