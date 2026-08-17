package app.mata.gradup.controller.web;

import app.mata.gradup.repository.GradeDisputeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class WebDisputeController {

  private final GradeDisputeRepository gradeDisputeRepository;

  @GetMapping("/web/disputes")
  public String listDisputes(Model model) {
    model.addAttribute("disputes", gradeDisputeRepository.findAll());
    return "disputes/list";
  }
}
