package app.mata.gradup.controller.web;

import app.mata.gradup.repository.ExamRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class WebExamController {

  private final ExamRepository examRepository;

  @GetMapping("/web/exams")
  public String listExams(Model model) {
    model.addAttribute("exams", examRepository.findAll());
    return "exams/list";
  }
}
