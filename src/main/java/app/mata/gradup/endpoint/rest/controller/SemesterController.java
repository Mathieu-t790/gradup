package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.SemesterCreditValidationResponse;
import app.mata.gradup.security.authorization.AuthorizationUtils;
import app.mata.gradup.service.SemesterService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class SemesterController {

  private final SemesterService semesterService;

  @PostMapping("/semesters/{semesterId}/finalize")
  @ResponseStatus(HttpStatus.CREATED)
  public SemesterCreditValidationResponse finalizeSemesterCredits(
      @PathVariable UUID semesterId, @RequestParam UUID trackId, Authentication authentication) {
    var userId =
        AuthorizationUtils.userDetails(authentication)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"))
            .userId();
    return semesterService.finalizeSemesterCredits(semesterId, trackId, userId);
  }
}
