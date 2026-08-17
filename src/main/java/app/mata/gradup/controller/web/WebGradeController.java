package app.mata.gradup.controller.web;

import app.mata.gradup.repository.GradeRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class WebGradeController {

  private final GradeRepository gradeRepository;

  @GetMapping("/web/grades")
  public String listGrades(Model model) {
    var grades = gradeRepository.findAll(PageRequest.of(0, 50));
    model.addAttribute("grades", grades.getContent());
    return "grades/list";
  }
}
