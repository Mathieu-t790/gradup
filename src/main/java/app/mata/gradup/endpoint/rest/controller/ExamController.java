package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.ExamCreateRequest;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.endpoint.rest.model.ExamUpdateRequest;
import app.mata.gradup.service.ExamService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ExamController {

  private final ExamService examService;

  @GetMapping("/exams/{examId}")
  public ExamResponse getExam(@PathVariable UUID examId) {
    return examService.getExam(examId);
  }

  @PostMapping("/course-offerings/{offeringId}/exams")
  @ResponseStatus(HttpStatus.CREATED)
  public ExamResponse createExam(
      @PathVariable UUID offeringId, @RequestBody @Valid ExamCreateRequest request) {
    return examService.createExam(offeringId, request);
  }

  @PatchMapping("/exams/{examId}")
  public ExamResponse updateExam(
      @PathVariable UUID examId, @RequestBody @Valid ExamUpdateRequest request) {
    return examService.updateExam(examId, request);
  }
}
