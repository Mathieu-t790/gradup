package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.CohortResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaPageResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaResponse;
import app.mata.gradup.endpoint.rest.model.StudentPageResponse;
import app.mata.gradup.endpoint.rest.model.StudentSummaryResponse;
import app.mata.gradup.endpoint.rest.model.TrackCode;
import app.mata.gradup.service.utils.TrackCodes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PromotionViewService {

  private static final int PAGE_SIZE = 200;

  private final CohortService cohortService;
  private final DiplomaService diplomaService;
  private final StudentService studentService;

  @Transactional(readOnly = true)
  public List<PromotionRow> promotionRows() {
    return cohortService.listCohorts().stream()
        .sorted(
            Comparator.comparingInt(CohortResponse::getExpectedGraduationYear)
                .reversed()
                .thenComparing(CohortResponse::getLabel))
        .map(cohort -> new PromotionRow(cohort, finished(cohort), graduatesFor(cohort)))
        .toList();
  }

  @Transactional(readOnly = true)
  public PromotionDetail promotionDetail(UUID cohortId, String track) {
    CohortResponse cohort = cohortService.getCohort(cohortId);
    boolean finished = finished(cohort);
    List<DiplomaResponse> promotionDiplomas = finished ? allDiplomas(cohortId, null) : List.of();
    List<DiplomaResponse> graduates =
        finished ? trackDiplomas(cohortId, track, promotionDiplomas) : List.of();
    Set<UUID> graduatedStudentIds =
        promotionDiplomas.stream()
            .map(diploma -> diploma.getStudent().getId())
            .collect(Collectors.toSet());
    List<StudentRow> students =
        allStudents(cohortId).stream()
            .map(student -> new StudentRow(student, graduatedStudentIds.contains(student.getId())))
            .toList();
    return new PromotionDetail(
        cohort, finished, track, graduates, graduates.size(), averageOf(graduates), students);
  }

  public boolean finished(CohortResponse cohort) {
    return LocalDate.now().getYear() > cohort.getExpectedGraduationYear();
  }

  public Map<String, Long> trackCounts() {
    Map<String, Long> counts = new HashMap<>(diplomaService.countDiplomasByTrack());
    counts.putIfAbsent("EL", 0L);
    counts.putIfAbsent("TN", 0L);
    return counts;
  }

  private long graduatesFor(CohortResponse cohort) {
    return finished(cohort) ? diplomaService.countCohortDiplomas(cohort.getId()) : 0;
  }

  private List<DiplomaResponse> trackDiplomas(
      UUID cohortId, String track, List<DiplomaResponse> promotionDiplomas) {
    if (track == null) {
      return promotionDiplomas;
    }
    return allDiplomas(cohortId, TrackCodes.toRest(track));
  }

  private List<DiplomaResponse> allDiplomas(UUID cohortId, TrackCode trackCode) {
    List<DiplomaResponse> all = new ArrayList<>();
    int pageNumber = 0;
    DiplomaPageResponse page;
    do {
      page =
          diplomaService.listCohortDiplomas(
              cohortId, trackCode, PageRequest.of(pageNumber, PAGE_SIZE));
      all.addAll(page.getContent());
      pageNumber++;
    } while (!page.getLast());
    return all;
  }

  private List<StudentSummaryResponse> allStudents(UUID cohortId) {
    List<StudentSummaryResponse> all = new ArrayList<>();
    int pageNumber = 0;
    StudentPageResponse page;
    do {
      page = studentService.listStudents(cohortId, null, PageRequest.of(pageNumber, PAGE_SIZE));
      all.addAll(page.getContent());
      pageNumber++;
    } while (!page.getLast());
    return all;
  }

  private static String averageOf(List<DiplomaResponse> diplomas) {
    return diplomas.stream()
        .map(DiplomaResponse::getOverallAverage)
        .filter(Objects::nonNull)
        .mapToDouble(Double::doubleValue)
        .average()
        .stream()
        .mapToObj(value -> String.format(Locale.FRENCH, "%.2f", value))
        .findFirst()
        .orElse("—");
  }

  public record PromotionRow(CohortResponse cohort, boolean finished, long graduates) {}

  public record StudentRow(StudentSummaryResponse student, boolean graduated) {}

  public record PromotionDetail(
      CohortResponse cohort,
      boolean finished,
      String track,
      List<DiplomaResponse> graduates,
      int graduateCount,
      String average,
      List<StudentRow> students) {}
}
