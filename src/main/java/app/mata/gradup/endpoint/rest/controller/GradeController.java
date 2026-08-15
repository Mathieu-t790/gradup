package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.GradeHistoryEntryResponse;
import app.mata.gradup.endpoint.rest.model.GradeResponse;
import app.mata.gradup.service.GradeService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GradeController {

  private final GradeService gradeService;

  @GetMapping("/exams/{examId}/grades")
  public List<GradeResponse> listExamGrades(@PathVariable UUID examId) {
    return gradeService.listExamGrades(examId);
  }

  @GetMapping("/grades/{gradeId}/history")
  public List<GradeHistoryEntryResponse> listGradeHistory(@PathVariable UUID gradeId) {
    return gradeService.listGradeHistory(gradeId);
  }
}
