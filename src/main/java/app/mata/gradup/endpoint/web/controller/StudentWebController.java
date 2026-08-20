package app.mata.gradup.endpoint.web.controller;

import app.mata.gradup.security.userDetails.JUserDetails;
import app.mata.gradup.service.StudentViewService;
import app.mata.gradup.service.utils.StudentLabels;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class StudentWebController {

  private final StudentViewService viewService;

  @GetMapping("/student/grades")
  public String studentGrades(
      @AuthenticationPrincipal JUserDetails userDetails,
      @RequestParam(required = false) UUID semesterId,
      Model model) {
    var view = viewService.studentGrades(userDetails.userId(), semesterId);
    model.addAttribute("grades", view.grades());
    model.addAttribute("semesters", view.semesters());
    model.addAttribute("selectedSemesterId", view.selectedSemesterId());
    StudentLabels.addGrades(model);
    return "student/grades";
  }
}
