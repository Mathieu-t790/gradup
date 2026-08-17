package app.mata.gradup.controller.web;

import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.model.JAcademicYear;
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

  private final AcademicYearRepository academicYearRepository;

  @GetMapping("/academic-years")
  public String listAcademicYears(Model model) {
    model.addAttribute("academicYears", academicYearRepository.findAll());
    return "academic-years/list";
  }

  @PostMapping("/academic-years")
  public String createAcademicYear(
      @RequestParam String label,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate) {
    var year = JAcademicYear.builder().label(label).startDate(startDate).endDate(endDate).build();
    academicYearRepository.save(year);
    return "redirect:/academic-years";
  }
}
