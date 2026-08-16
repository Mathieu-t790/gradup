package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.service.ExamService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ExamController {

  private final ExamService examService;

  @GetMapping("/exams/{examId}")
  public ExamResponse getExam(@PathVariable UUID examId) {
    return examService.getExam(examId);
  }
}
