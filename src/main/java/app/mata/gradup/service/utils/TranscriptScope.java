package app.mata.gradup.service.utils;

import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JStudentGroupHistory;
import app.mata.gradup.repository.model.JStudentTrackHistory;
import app.mata.gradup.repository.model.JTrack;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TranscriptScope {

  private final CourseOfferingRepository courseOfferingRepository;
  private final SemesterRepository semesterRepository;
  private final StudentGroupHistoryRepository studentGroupHistoryRepository;
  private final StudentTrackHistoryRepository studentTrackHistoryRepository;

  public List<JCourseOffering> forSemester(JStudent student, JSemester semester) {
    JGroup group = groupAt(student, semester.getStartDate());
    return group == null ? List.of() : byGroupSemesters(group.getId(), List.of(semester.getId()));
  }

  public List<JCourseOffering> forYear(JStudent student, JAcademicYear year) {
    List<JCourseOffering> offerings = new ArrayList<>();
    for (JSemester semester : semesterRepository.findByAcademicYearId(year.getId())) {
      JGroup group = groupAt(student, semester.getStartDate());
      if (group != null) {
        offerings.addAll(byGroupSemesters(group.getId(), List.of(semester.getId())));
      }
    }
    return offerings;
  }

  public List<JCourseOffering> byIds(Collection<UUID> offeringIds) {
    return offeringIds.isEmpty()
        ? List.of()
        : new ArrayList<>(courseOfferingRepository.findAllById(offeringIds));
  }

  public JGroup groupAt(JStudent student, LocalDate date) {
    return studentGroupHistoryRepository
        .findByStudentIdOrderByStartDateDesc(student.getId())
        .stream()
        .filter(history -> activeAt(history, date))
        .map(JStudentGroupHistory::getGroup)
        .findFirst()
        .orElse(null);
  }

  public JTrack trackAt(JStudent student, LocalDate date) {
    return studentTrackHistoryRepository
        .findByStudentIdOrderByStartDateDesc(student.getId())
        .stream()
        .filter(history -> activeAt(history, date))
        .map(JStudentTrackHistory::getTrack)
        .findFirst()
        .orElse(null);
  }

  private List<JCourseOffering> byGroupSemesters(UUID groupId, Collection<UUID> semesterIds) {
    return Pages.allPages(
        pageable ->
            courseOfferingRepository.findByGroupIdAndSemesterIdIn(groupId, semesterIds, pageable),
        Pages.DEFAULT_PAGE_SIZE);
  }

  private static boolean activeAt(JStudentGroupHistory history, LocalDate date) {
    return !history.getStartDate().isAfter(date)
        && (history.getEndDate() == null || !history.getEndDate().isBefore(date));
  }

  private static boolean activeAt(JStudentTrackHistory history, LocalDate date) {
    return !history.getStartDate().isAfter(date)
        && (history.getEndDate() == null || !history.getEndDate().isBefore(date));
  }
}
