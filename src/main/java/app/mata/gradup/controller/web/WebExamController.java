package app.mata.gradup.controller.web;

import app.mata.gradup.service.ExamService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class WebExamController {

  private final ExamService examService;

  @GetMapping("/web/exams")
  public String listExams(Model model) {
    model.addAttribute("exams", examService.listForWeb());
    return "exams/list";
  }
}
