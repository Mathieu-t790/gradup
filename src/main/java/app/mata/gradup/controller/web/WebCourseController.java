package app.mata.gradup.controller.web;

import app.mata.gradup.service.CourseService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class WebCourseController {

  private final CourseService courseService;

  @GetMapping("/web/courses")
  public String listCourses(Model model) {
    model.addAttribute("courses", courseService.listForWeb());
    return "courses/list";
  }
}
