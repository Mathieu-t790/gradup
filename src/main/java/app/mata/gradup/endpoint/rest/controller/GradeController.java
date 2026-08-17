package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.GradeCreateRequest;
import app.mata.gradup.endpoint.rest.model.GradeResponse;
import app.mata.gradup.endpoint.rest.model.GradeUpdateRequest;
import app.mata.gradup.security.userDetails.JUserDetails;
import app.mata.gradup.service.GradeService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GradeController {

  private final GradeService gradeService;

  @PostMapping("/exams/{examId}/grades")
  @ResponseStatus(HttpStatus.CREATED)
  public GradeResponse recordGrade(
      @PathVariable UUID examId,
      @RequestBody GradeCreateRequest request,
      @AuthenticationPrincipal JUserDetails userDetails) {
    return gradeService.recordGrade(examId, request, userDetails.userId());
  }

  @PutMapping("/grades/{gradeId}")
  public GradeResponse updateGrade(
      @PathVariable UUID gradeId,
      @RequestBody GradeUpdateRequest request,
      @AuthenticationPrincipal JUserDetails userDetails) {
    return gradeService.updateGrade(gradeId, request, userDetails.userId());
  }
}
