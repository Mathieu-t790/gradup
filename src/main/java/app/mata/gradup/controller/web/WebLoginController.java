package app.mata.gradup.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebLoginController {

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/access-denied")
  public String accessDenied() {
    return "access-denied";
  }
}
