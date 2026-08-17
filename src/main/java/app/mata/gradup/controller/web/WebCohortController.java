package app.mata.gradup.controller.web;

import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.model.JCohort;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class WebCohortController {

  private final CohortRepository cohortRepository;

  @GetMapping("/web/cohorts")
  public String listCohorts(Model model) {
    model.addAttribute("cohorts", cohortRepository.findAll());
    return "cohorts/list";
  }

  @PostMapping("/web/cohorts")
  public String createCohort(
      @RequestParam String label,
      @RequestParam int entryYear,
      @RequestParam int expectedGraduationYear) {
    var cohort =
        JCohort.builder()
            .label(label)
            .entryYear(entryYear)
            .expectedGraduationYear(expectedGraduationYear)
            .build();
    cohortRepository.save(cohort);
    return "redirect:/web/cohorts";
  }
}
