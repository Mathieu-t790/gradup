package app.mata.gradup.controller.web;

import app.mata.gradup.endpoint.rest.model.AcademicYearCreateRequest;
import app.mata.gradup.service.AcademicYearService;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class WebAcademicYearController {

  private final AcademicYearService academicYearService;

  @GetMapping("/web/academic-years")
  public String listAcademicYears(Model model) {
    model.addAttribute("academicYears", academicYearService.listForWeb());
    return "academic-years/list";
  }

  @PostMapping("/web/academic-years")
  public String createAcademicYear(
      @RequestParam String label,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate) {
    academicYearService.createAcademicYear(
        new AcademicYearCreateRequest().label(label).startDate(startDate).endDate(endDate));
    return "redirect:/web/academic-years";
  }
}
