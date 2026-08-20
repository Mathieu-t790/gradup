package app.mata.gradup.endpoint.web.controller;

import app.mata.gradup.endpoint.rest.model.CohortResponse;
import app.mata.gradup.endpoint.rest.model.DiplomaExportResponse;
import app.mata.gradup.endpoint.rest.model.TrackCode;
import app.mata.gradup.service.CohortService;
import app.mata.gradup.service.DiplomaService;
import app.mata.gradup.service.utils.TrackCodes;
import app.mata.gradup.service.utils.Wording;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  @GetMapping("/")
  public String home(Authentication authentication, Model model) {
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      addLandingLabels(model);
      return "landing";
    }
    return "redirect:/promotions";
  }

  @GetMapping("/promotions")
  public String promotions(Model model) {
    List<PromotionRow> rows =
        cohortService.listCohorts().stream()
            .sorted(
                Comparator.comparingInt(CohortResponse::getExpectedGraduationYear)
                    .reversed()
                    .thenComparing(CohortResponse::getLabel))
            .map(cohort -> new PromotionRow(cohort, diplomaService.countCohortDiplomas(cohort.getId())))
            .toList();
    model.addAttribute("rows", rows);
    model.addAttribute("promotionCount", rows.size());
    model.addAttribute("graduateCount", rows.stream().mapToLong(PromotionRow::graduates).sum());
    model.addAttribute("trackCounts", trackCounts());
    addPromotionLabels(model);
    return "promotions/list";
  }

  @PostMapping("/promotions/{cohortId}/diplomas")
  public String diplomas(
      @PathVariable UUID cohortId,
      @RequestParam String action,
      @RequestParam(required = false) String track) {
    TrackCode trackCode = TrackCodes.toRest(track);
    if ("generate".equals(action)) {
      diplomaService.generateCohortDiplomas(cohortId, trackCode);
      return "redirect:/promotions";
    }
    DiplomaExportResponse export = diplomaService.exportCohortDiplomas(cohortId, trackCode);
    return "redirect:" + export.getDownloadUrl();
  }

  private Map<String, Long> trackCounts() {
    Map<String, Long> counts = new HashMap<>(diplomaService.countDiplomasByTrack());
    counts.putIfAbsent("EL", 0L);
    counts.putIfAbsent("TN", 0L);
    return counts;
  }

  private void addLandingLabels(Model model) {
    model.addAttribute("appName", Wording.get("promotion.web.app.name"));
    model.addAttribute("landingTitle", Wording.get("landing.title"));
    model.addAttribute("landingSubtitle", Wording.get("landing.subtitle"));
    model.addAttribute("loginLabel", Wording.get("landing.login"));
    model.addAttribute("accessNote", Wording.get("landing.access.note"));
    model.addAttribute("footerLabel", Wording.get("landing.footer"));
  }

  private void addPromotionLabels(Model model) {
    model.addAttribute("appName", Wording.get("promotion.web.app.name"));
    model.addAttribute("pageTitle", Wording.get("promotion.web.page.title"));
    model.addAttribute("pageSubtitle", Wording.get("promotion.web.page.subtitle"));
    model.addAttribute("columnLabel", Wording.get("promotion.web.table.label"));
    model.addAttribute("columnEntryYear", Wording.get("promotion.web.table.entry.year"));
    model.addAttribute("columnGraduationYear", Wording.get("promotion.web.table.graduation.year"));
    model.addAttribute("columnGraduates", Wording.get("promotion.web.table.graduates"));
    model.addAttribute("columnActions", Wording.get("promotion.web.table.actions"));
    model.addAttribute("filterLabel", Wording.get("promotion.web.filter.label"));
    model.addAttribute("filterAll", Wording.get("promotion.web.filter.all"));
    model.addAttribute("searchPlaceholder", Wording.get("promotion.web.search.placeholder"));
    model.addAttribute("generateLabel", Wording.get("promotion.web.generate"));
    model.addAttribute("downloadLabel", Wording.get("promotion.web.download"));
    model.addAttribute("logoutLabel", Wording.get("promotion.web.logout"));
    model.addAttribute("emptyTitle", Wording.get("promotion.web.empty.title"));
    model.addAttribute("emptyText", Wording.get("promotion.web.empty.text"));
    model.addAttribute("statPromotions", Wording.get("promotion.web.stat.promotions"));
    model.addAttribute("statGraduates", Wording.get("promotion.web.stat.graduates"));
    model.addAttribute("statTrackEl", Wording.get("promotion.web.stat.track", "EL"));
    model.addAttribute("statTrackTn", Wording.get("promotion.web.stat.track", "TN"));
  }

  public record PromotionRow(CohortResponse cohort, long graduates) {}
}