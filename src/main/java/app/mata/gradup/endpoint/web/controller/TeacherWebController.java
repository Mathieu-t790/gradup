package app.mata.gradup.endpoint.web.controller;

import app.mata.gradup.security.userDetails.JUserDetails;
import app.mata.gradup.service.TeacherViewService;
import app.mata.gradup.service.utils.TeacherLabels;
import app.mata.gradup.service.utils.TeacherWeb;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class TeacherWebController {

  private final TeacherViewService viewService;

  @GetMapping("/teacher/courses")
  public String courses(
      @AuthenticationPrincipal JUserDetails userDetails,
      @RequestParam(required = false) UUID semesterId,
      Model model) {
    var view = viewService.teacherCourses(userDetails.userId());
    model.addAttribute("offerings", view.offerings());
    model.addAttribute("semesters", view.semesters());
    model.addAttribute("selectedSemesterId", semesterId);
    TeacherLabels.addCourses(model);
    return "teacher/courses";
  }

  @GetMapping("/teacher/courses/{offeringId}")
  public String offering(
      @AuthenticationPrincipal JUserDetails userDetails,
      @PathVariable UUID offeringId,
      @RequestParam(required = false) String error,
      Model model) {
    var view = viewService.teacherOffering(userDetails.userId(), offeringId);
    model.addAttribute("view", view);
    model.addAttribute("errorMessage", TeacherWeb.errorMessage(error));
    TeacherLabels.addOffering(model);
    return "teacher/offering";
  }

  @PostMapping("/teacher/courses/{offeringId}/exams/{examId}/grades")
  public String recordGrade(
      @AuthenticationPrincipal JUserDetails userDetails,
      @PathVariable UUID offeringId,
      @PathVariable UUID examId,
      @RequestParam UUID studentId,
      @RequestParam String score) {
    Double parsed = TeacherWeb.parseScore(score);
    if (parsed == null) {
      return "redirect:/teacher/courses/" + offeringId + "?error=score.invalid";
    }
    viewService.recordGrade(userDetails.userId(), offeringId, examId, studentId, parsed);
    return "redirect:/teacher/courses/" + offeringId;
  }

  @PostMapping("/teacher/courses/grades/{gradeId}")
  public String updateGrade(
      @AuthenticationPrincipal JUserDetails userDetails,
      @PathVariable UUID gradeId,
      @RequestParam String score,
      @RequestParam(required = false) String reason) {
    UUID offeringId = viewService.offeringIdForGrade(userDetails.userId(), gradeId);
    Double parsed = TeacherWeb.parseScore(score);
    if (parsed == null) {
      return "redirect:/teacher/courses/" + offeringId + "?error=score.invalid";
    }
    viewService.updateGrade(userDetails.userId(), gradeId, parsed, reason);
    return "redirect:/teacher/courses/" + offeringId;
  }
}
