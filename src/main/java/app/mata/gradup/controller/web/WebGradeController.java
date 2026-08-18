package app.mata.gradup.controller.web;

import app.mata.gradup.service.GradeService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class WebGradeController {

  private final GradeService gradeService;

  @GetMapping("/web/grades")
  public String listGrades(Model model) {
    model.addAttribute("grades", gradeService.listForWeb(PageRequest.of(0, 50)));
    return "grades/list";
  }
}
