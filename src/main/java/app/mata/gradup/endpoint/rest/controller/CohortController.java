package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.CohortCreateRequest;
import app.mata.gradup.endpoint.rest.model.CohortResponse;
import app.mata.gradup.service.CohortService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class CohortController {

  private final CohortService cohortService;

  @GetMapping("/cohorts")
  public List<CohortResponse> listCohorts() {
    return cohortService.listCohorts();
  }

  @PostMapping("/cohorts")
  @ResponseStatus(HttpStatus.CREATED)
  public CohortResponse createCohort(@RequestBody @Valid CohortCreateRequest request) {
    return cohortService.createCohort(request);
  }

  @GetMapping("/cohorts/{cohortId}")
  public CohortResponse getCohort(@PathVariable UUID cohortId) {
    return cohortService.getCohort(cohortId);
  }
}
