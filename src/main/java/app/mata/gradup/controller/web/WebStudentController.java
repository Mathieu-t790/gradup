package app.mata.gradup.controller.web;

import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class WebStudentController {

  private final StudentRepository studentRepository;
  private final StudentGroupHistoryRepository studentGroupHistoryRepository;
  private final GradeRepository gradeRepository;

  @GetMapping("/students")
  public String listStudents(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Model model) {
    var pageable = PageRequest.of(page, size);
    var studentsPage = studentRepository.findAll(pageable);
    model.addAttribute("students", studentsPage.getContent());
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", studentsPage.getTotalPages());
    model.addAttribute("pageSize", size);
    return "students/list";
  }

  @GetMapping("/students/{studentId}")
  public String getStudent(@PathVariable java.util.UUID studentId, Model model) {
    var student = studentRepository.findById(studentId).orElseThrow();
    var groupHistory = studentGroupHistoryRepository.findByStudentIdOrderByStartDateDesc(studentId);
    var gradesPage = gradeRepository.findByStudentId(studentId, PageRequest.of(0, 10));
    model.addAttribute("student", student);
    model.addAttribute("groupHistory", groupHistory);
    model.addAttribute("grades", gradesPage.getContent());
    return "students/detail";
  }
}
