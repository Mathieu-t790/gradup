package app.mata.gradup.endpoint.web.controller;

import app.mata.gradup.endpoint.rest.model.CohortCreateRequest;
import app.mata.gradup.endpoint.rest.model.CohortResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaExportResponse;
import app.mata.gradup.service.CohortService;
import app.mata.gradup.service.DiplomaService;
import app.mata.gradup.service.PromotionViewService;
import app.mata.gradup.service.PromotionViewService.PromotionRow;
import app.mata.gradup.service.utils.PromotionLabels;
import app.mata.gradup.service.utils.PromotionWeb;
import app.mata.gradup.service.utils.PromotionWeb.PromotionForm;
import app.mata.gradup.service.utils.TrackCodes;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class PromotionController {

  private final CohortService cohortService;
  private final DiplomaService diplomaService;
  private final PromotionViewService viewService;

  @GetMapping("/")
  public String home(Authentication authentication, Model model) {
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      PromotionLabels.addLanding(model);
      return "landing";
    }
    return "redirect:/promotions";
  }

  @GetMapping("/promotions")
  public String promotions(@RequestParam(required = false) String error, Model model) {
    var rows = viewService.promotionRows();
    model.addAttribute("rows", rows);
    model.addAttribute("promotionCount", rows.size());
    model.addAttribute("graduateCount", rows.stream().mapToLong(PromotionRow::graduates).sum());
    model.addAttribute("trackCounts", viewService.trackCounts());
    model.addAttribute("errorMessage", PromotionWeb.errorMessage(error));
    PromotionLabels.addList(model);
    return "promotions/list";
  }

  @PostMapping("/promotions")
  public String createPromotion(
      @RequestParam String label,
      @RequestParam String entryYear,
      @RequestParam String expectedGraduationYear) {
    PromotionForm form = PromotionForm.of(label, entryYear, expectedGraduationYear);
    if (form.error() != null || form.request().getLabel() == null) {
      return PromotionWeb.redirectListError(form.error() != null ? form.error() : "label.required");
    }
    CohortResponse created =
        cohortService.createCohort(
            new CohortCreateRequest()
                .label(form.request().getLabel())
                .entryYear(form.request().getEntryYear())
                .expectedGraduationYear(form.request().getExpectedGraduationYear()));
    return "redirect:/promotions/" + created.getId();
  }

  @PostMapping("/promotions/{cohortId}/edit")
  public String editPromotion(
      @PathVariable UUID cohortId,
      @RequestParam(required = false) String label,
      @RequestParam(required = false) String entryYear,
      @RequestParam(required = false) String expectedGraduationYear) {
    PromotionForm form = PromotionForm.of(label, entryYear, expectedGraduationYear);
    if (form.error() != null) {
      return PromotionWeb.redirectListError(form.error());
    }
    cohortService.updateCohort(cohortId, form.request());
    return "redirect:/promotions";
  }

  @GetMapping("/promotions/{cohortId}")
  public String promotionDetail(
      @PathVariable UUID cohortId,
      @RequestParam(required = false) String track,
      @RequestParam(required = false) String error,
      Model model) {
    var detail = viewService.promotionDetail(cohortId, track);
    model.addAttribute("detail", detail);
    model.addAttribute("errorMessage", PromotionWeb.errorMessage(error));
    PromotionLabels.addDetail(model, detail.cohort().getExpectedGraduationYear() + 1);
    return "promotions/detail";
  }

  @PostMapping("/promotions/{cohortId}/diplomas")
  public String diplomas(
      @PathVariable UUID cohortId,
      @RequestParam String action,
      @RequestParam(required = false) String track) {
    CohortResponse cohort = cohortService.getCohort(cohortId);
    if (!viewService.finished(cohort)) {
      return PromotionWeb.redirectDetail(cohortId, track, "not.finished");
    }
    if ("generate".equals(action)) {
      diplomaService.generateCohortDiplomas(cohortId, TrackCodes.toRest(track));
      return PromotionWeb.redirectDetail(cohortId, track, null);
    }
    DiplomaExportResponse export =
        diplomaService.exportCohortDiplomas(cohortId, TrackCodes.toRest(track));
    return "redirect:" + export.getDownloadUrl();
  }
}
