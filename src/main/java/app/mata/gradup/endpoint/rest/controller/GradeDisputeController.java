package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.GradeDisputeCreateRequest;
import app.mata.gradup.endpoint.rest.model.GradeDisputePageResponse;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResolveRequest;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResponse;
import app.mata.gradup.security.userDetails.JUserDetails;
import app.mata.gradup.service.GradeDisputeService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GradeDisputeController {

  private final GradeDisputeService gradeDisputeService;

  @PostMapping("/grades/{gradeId}/disputes")
  @ResponseStatus(HttpStatus.CREATED)
  public GradeDisputeResponse createGradeDispute(
      @PathVariable UUID gradeId,
      @RequestBody GradeDisputeCreateRequest request,
      @AuthenticationPrincipal JUserDetails userDetails) {
    return gradeDisputeService.createGradeDispute(gradeId, request, userDetails.userId());
  }

  @PatchMapping("/disputes/{disputeId}")
  public GradeDisputeResponse resolveDispute(
      @PathVariable UUID disputeId,
      @RequestBody GradeDisputeResolveRequest request,
      @AuthenticationPrincipal JUserDetails userDetails) {
    return gradeDisputeService.resolveDispute(disputeId, request, userDetails.userId());
  }

  @GetMapping("/students/{studentId}/disputes")
  public List<GradeDisputeResponse> listStudentDisputes(@PathVariable UUID studentId) {
    return gradeDisputeService.listStudentDisputes(studentId);
  }

  @GetMapping("/disputes")
  public GradeDisputePageResponse listDisputes(
      @RequestParam(required = false) app.mata.gradup.endpoint.rest.model.DisputeStatus status,
      Pageable pageable,
      @AuthenticationPrincipal JUserDetails userDetails) {
    app.mata.gradup.model.DisputeStatus domainStatus =
        status == null ? null : app.mata.gradup.model.DisputeStatus.valueOf(status.name());
    return gradeDisputeService.listDisputes(
        domainStatus, pageable, userDetails.userId(), userDetails.getRole());
  }
}
