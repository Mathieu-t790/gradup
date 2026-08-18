package app.mata.gradup.controller.web;

import app.mata.gradup.endpoint.rest.model.TranscriptGenerateRequest;
import app.mata.gradup.endpoint.rest.model.TranscriptType;
import app.mata.gradup.service.GradeService;
import app.mata.gradup.service.SemesterService;
import app.mata.gradup.service.StudentService;
import app.mata.gradup.service.TranscriptService;
import java.net.URI;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class WebStudentController {

  private final StudentService studentService;
  private final GradeService gradeService;
  private final SemesterService semesterService;
  private final TranscriptService transcriptService;

  @GetMapping("/web/students")
  public String listStudents(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Model model) {
    var pageable = PageRequest.of(page, size);
    var studentsPage = studentService.listForWeb(pageable);
    model.addAttribute("students", studentsPage.getContent());
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", studentsPage.getTotalPages());
    model.addAttribute("pageSize", size);
    return "students/list";
  }

  @GetMapping("/web/students/{studentId}")
  public String getStudent(@PathVariable UUID studentId, Model model) {
    model.addAttribute("student", studentService.getForWeb(studentId));
    model.addAttribute("groupHistory", studentService.groupHistoryForWeb(studentId));
    model.addAttribute(
        "grades", gradeService.listForWebByStudent(studentId, PageRequest.of(0, 10)));
    model.addAttribute("semesters", semesterService.listForWeb());
    return "students/detail";
  }

  @PostMapping("/web/students/{studentId}/transcripts")
  public ResponseEntity<Void> generateTranscript(
      @PathVariable UUID studentId, @RequestParam UUID semesterId) {
    var transcript =
        transcriptService.generateStudentTranscript(
            studentId,
            new TranscriptGenerateRequest()
                .type(TranscriptType.PROVISIONAL)
                .semesterId(semesterId));
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(transcript.getDownloadUrl()))
        .build();
  }
}
