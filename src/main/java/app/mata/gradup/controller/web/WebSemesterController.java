package app.mata.gradup.controller.web;

import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.model.JSemester;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class WebSemesterController {

  private final SemesterRepository semesterRepository;
  private final AcademicYearRepository academicYearRepository;

  @GetMapping("/semesters")
  public String listSemesters(Model model) {
    model.addAttribute("semesters", semesterRepository.findAll());
    model.addAttribute("academicYears", academicYearRepository.findAll());
    return "semesters/list";
  }

  @PostMapping("/semesters")
  public String createSemester(
      @RequestParam int number,
      @RequestParam UUID academicYearId,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate) {
    var academicYear = academicYearRepository.findById(academicYearId).orElseThrow();
    var semester =
        JSemester.builder()
            .number(number)
            .academicYear(academicYear)
            .startDate(startDate)
            .endDate(endDate)
            .build();
    semesterRepository.save(semester);
    return "redirect:/semesters";
  }
}
