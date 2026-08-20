package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.SemesterResponse;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class StudentViewService {

  private static final int PAGE_SIZE = 200;
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

  private final GradeService gradeService;
  private final SemesterService semesterService;

  @Transactional(readOnly = true)
  public StudentGradesView studentGrades(UUID studentId, UUID semesterId) {
    var page = gradeService.listStudentGrades(studentId, semesterId, PageRequest.of(0, PAGE_SIZE));
    var rows =
        page.getContent().stream()
            .map(
                grade ->
                    new StudentGradeRow(
                        grade.getCourseReference(),
                        grade.getExamLabel(),
                        grade.getScore(),
                        grade.getRecordedByName(),
                        DATE_TIME_FORMATTER.format(grade.getRecordedAt())))
            .toList();
    return new StudentGradesView(rows, semesterService.listSemesters(null), semesterId);
  }

  public record StudentGradesView(
      List<StudentGradeRow> grades, List<SemesterResponse> semesters, UUID selectedSemesterId) {}

  public record StudentGradeRow(
      String courseReference,
      String examLabel,
      Double score,
      String recordedByName,
      String recordedAt) {}
}
