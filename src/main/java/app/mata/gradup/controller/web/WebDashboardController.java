package app.mata.gradup.controller.web;

import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GradeDisputeRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.StudentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class WebDashboardController {

  private final StudentRepository studentRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final GradeDisputeRepository gradeDisputeRepository;
  private final CohortRepository cohortRepository;
  private final AcademicYearRepository academicYearRepository;
  private final SemesterRepository semesterRepository;
  private final GradeRepository gradeRepository;

  @GetMapping("/")
  public String dashboard(Model model) {
    model.addAttribute("studentCount", studentRepository.count());
    model.addAttribute("courseCount", courseRepository.count());
    model.addAttribute("examCount", examRepository.count());
    model.addAttribute("disputeCount", gradeDisputeRepository.count());
    model.addAttribute("cohortCount", cohortRepository.count());
    model.addAttribute("academicYearCount", academicYearRepository.count());
    model.addAttribute("semesterCount", semesterRepository.count());
    model.addAttribute("gradeCount", gradeRepository.count());
    return "dashboard";
  }
}
