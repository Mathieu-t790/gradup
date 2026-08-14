package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.StudentCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentResponse;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentUpdateRequest;
import app.mata.gradup.endpoint.rest.model.TranscriptResponse;
import app.mata.gradup.service.StudentHistoryService;
import app.mata.gradup.service.StudentService;
import app.mata.gradup.service.StudentTranscriptService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class StudentController {

  private final StudentService studentService;
  private final StudentHistoryService studentHistoryService;
  private final StudentTranscriptService studentTranscriptService;

  @PostMapping("/students")
  @ResponseStatus(HttpStatus.CREATED)
  public StudentResponse createStudent(@RequestBody @Valid StudentCreateRequest request) {
    return studentService.createStudent(request);
  }

  @PatchMapping("/students/{studentId}")
  public StudentResponse updateStudent(
      @PathVariable UUID studentId, @RequestBody @Valid StudentUpdateRequest request) {
    return studentService.updateStudent(studentId, request);
  }

  @GetMapping("/students/{studentId}/group-history")
  public List<StudentGroupHistoryResponse> listStudentGroupHistory(
      @PathVariable UUID studentId) {
    return studentHistoryService.listStudentGroupHistory(studentId);
  }

  @GetMapping("/students/{studentId}/track-history")
  public List<StudentTrackHistoryResponse> listStudentTrackHistory(
      @PathVariable UUID studentId) {
    return studentHistoryService.listStudentTrackHistory(studentId);
  }

  @GetMapping("/students/{studentId}/transcripts")
  public List<TranscriptResponse> listStudentTranscripts(@PathVariable UUID studentId) {
    return studentTranscriptService.listStudentTranscripts(studentId);
  }
}
