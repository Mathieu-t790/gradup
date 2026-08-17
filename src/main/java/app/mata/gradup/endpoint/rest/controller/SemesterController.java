package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.SemesterCreateRequest;
import app.mata.gradup.endpoint.rest.model.SemesterCreditValidationResponse;
import app.mata.gradup.endpoint.rest.model.SemesterResponse;
import app.mata.gradup.security.userDetails.JUserDetails;
import app.mata.gradup.service.SemesterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class SemesterController {

  private final SemesterService semesterService;

  @GetMapping("/semesters")
  public List<SemesterResponse> listSemesters(
      @RequestParam(value = "academicYearId", required = false) UUID academicYearId) {
    return semesterService.listSemesters(academicYearId);
  }

  @PostMapping("/semesters")
  @ResponseStatus(HttpStatus.CREATED)
  public SemesterResponse createSemester(@Valid @RequestBody SemesterCreateRequest request) {
    return semesterService.createSemester(request);
  }

  @PostMapping("/semesters/{semesterId}/finalize")
  @ResponseStatus(HttpStatus.CREATED)
  public SemesterCreditValidationResponse finalizeSemesterCredits(
      @PathVariable UUID semesterId,
      @RequestParam UUID trackId,
      @AuthenticationPrincipal JUserDetails userDetails) {
    return semesterService.finalizeSemesterCredits(semesterId, trackId, userDetails.userId());
  }
}
