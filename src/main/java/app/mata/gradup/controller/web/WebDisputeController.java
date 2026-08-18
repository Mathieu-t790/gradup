package app.mata.gradup.controller.web;

import app.mata.gradup.service.GradeDisputeService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class WebDisputeController {

  private final GradeDisputeService gradeDisputeService;

  @GetMapping("/web/disputes")
  public String listDisputes(Model model) {
    model.addAttribute("disputes", gradeDisputeService.listForWeb());
    return "disputes/list";
  }
}
