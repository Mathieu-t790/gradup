package app.mata.gradup.controller.web;

import app.mata.gradup.endpoint.rest.model.CohortCreateRequest;
import app.mata.gradup.service.CohortService;
import app.mata.gradup.service.DiplomaService;
import java.net.URI;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class WebCohortController {

  private final CohortService cohortService;
  private final DiplomaService diplomaService;

  @GetMapping("/web/cohorts")
  public String listCohorts(Model model) {
    model.addAttribute("cohorts", cohortService.listCohorts());
    return "cohorts/list";
  }

  @PostMapping("/web/cohorts")
  public String createCohort(
      @RequestParam String label,
      @RequestParam int entryYear,
      @RequestParam int expectedGraduationYear) {
    cohortService.createCohort(
        new CohortCreateRequest()
            .label(label)
            .entryYear(entryYear)
            .expectedGraduationYear(expectedGraduationYear));
    return "redirect:/web/cohorts";
  }

  @GetMapping("/web/cohorts/{cohortId}/diplomas/export")
  public ResponseEntity<Void> exportGraduates(@PathVariable UUID cohortId) {
    var export = diplomaService.exportCohortDiplomas(cohortId, null);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(export.getDownloadUrl()))
        .build();
  }
}
