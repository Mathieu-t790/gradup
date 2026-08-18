package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.DiplomaExportResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaPageResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaResponse;
import app.mata.gradup.service.DiplomaService;
import app.mata.gradup.service.utils.TrackCodes;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class DiplomaController {

  private final DiplomaService diplomaService;

  @GetMapping("/cohorts/{cohortId}/diplomas")
  public DiplomaPageResponse listCohortDiplomas(
      @PathVariable UUID cohortId,
      @RequestParam(required = false) String track,
      Pageable pageable) {
    return diplomaService.listCohortDiplomas(cohortId, TrackCodes.toRest(track), pageable);
  }

  @PostMapping("/cohorts/{cohortId}/diplomas/generate")
  public List<DiplomaResponse> generateCohortDiplomas(
      @PathVariable UUID cohortId, @RequestParam(required = false) String track) {
    return diplomaService.generateCohortDiplomas(cohortId, TrackCodes.toRest(track));
  }

  @GetMapping("/cohorts/{cohortId}/diplomas/export")
  public DiplomaExportResponse exportCohortDiplomas(
      @PathVariable UUID cohortId, @RequestParam(required = false) String track) {
    return diplomaService.exportCohortDiplomas(cohortId, TrackCodes.toRest(track));
  }
}
