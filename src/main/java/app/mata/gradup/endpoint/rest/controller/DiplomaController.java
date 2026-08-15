package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.DiplomaPageResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaResponse;
import app.mata.gradup.endpoint.rest.model.TrackCode;
import app.mata.gradup.service.DiplomaService;
import app.mata.gradup.service.utils.XlsxRenderer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    return diplomaService.listCohortDiplomas(cohortId, nullableTrack(track), pageable);
  }

  @PostMapping("/cohorts/{cohortId}/diplomas/generate")
  public List<DiplomaResponse> generateCohortDiplomas(
      @PathVariable UUID cohortId, @RequestParam TrackCode track) {
    return diplomaService.generateCohortDiplomas(cohortId, track);
  }

  @GetMapping("/cohorts/{cohortId}/diplomas/export")
  public ResponseEntity<byte[]> exportCohortDiplomas(
      @PathVariable UUID cohortId, @RequestParam(required = false) String track) {
    DiplomaService.ExportResult result =
        diplomaService.exportCohortDiplomas(cohortId, nullableTrack(track));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(XlsxRenderer.contentType()));
    headers.setContentDisposition(
        ContentDisposition.attachment()
            .filename(result.filename(), StandardCharsets.UTF_8)
            .build());
    return ResponseEntity.ok().headers(headers).body(result.content());
  }

  private static TrackCode nullableTrack(String track) {
    if (track == null || track.isBlank()) {
      return null;
    }
    return TrackCode.fromValue(track);
  }
}
