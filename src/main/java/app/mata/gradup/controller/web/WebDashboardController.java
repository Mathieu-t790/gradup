package app.mata.gradup.controller.web;

import app.mata.gradup.service.DashboardService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class WebDashboardController {

  private final DashboardService dashboardService;

  @GetMapping("/")
  public String dashboard(Model model) {
    DashboardService.Counts counts = dashboardService.counts();
    model.addAttribute("studentCount", counts.students());
    model.addAttribute("courseCount", counts.courses());
    model.addAttribute("examCount", counts.exams());
    model.addAttribute("disputeCount", counts.disputes());
    model.addAttribute("cohortCount", counts.cohorts());
    model.addAttribute("academicYearCount", counts.academicYears());
    model.addAttribute("semesterCount", counts.semesters());
    model.addAttribute("gradeCount", counts.grades());
    return "dashboard";
  }

  @GetMapping("/access-denied")
  public String accessDenied() {
    return "access-denied";
  }
}