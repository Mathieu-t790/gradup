package app.mata.gradup.controller.web;

import app.mata.gradup.endpoint.rest.model.SemesterCreateRequest;
import app.mata.gradup.service.AcademicYearService;
import app.mata.gradup.service.SemesterService;
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

  private final SemesterService semesterService;
  private final AcademicYearService academicYearService;

  @GetMapping("/web/semesters")
  public String listSemesters(Model model) {
    model.addAttribute("semesters", semesterService.listForWeb());
    model.addAttribute("academicYears", academicYearService.listAcademicYears());
    return "semesters/list";
  }

  @PostMapping("/web/semesters")
  public String createSemester(
      @RequestParam int number,
      @RequestParam UUID academicYearId,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate) {
    semesterService.createSemester(
        new SemesterCreateRequest()
            .number(number)
            .academicYearId(academicYearId)
            .startDate(startDate)
            .endDate(endDate));
    return "redirect:/web/semesters";
  }
}
