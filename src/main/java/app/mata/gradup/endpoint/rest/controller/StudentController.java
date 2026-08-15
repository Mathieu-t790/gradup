package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.GradePageResponse;
import app.mata.gradup.endpoint.rest.model.GraduationEligibilityResponse;
import app.mata.gradup.endpoint.rest.model.StudentCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentPageResponse;
import app.mata.gradup.endpoint.rest.model.StudentResponse;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentUpdateRequest;
import app.mata.gradup.service.GradeService;
import app.mata.gradup.service.GraduationEligibilityService;
import app.mata.gradup.service.StudentHistoryService;
import app.mata.gradup.service.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class StudentController {

  private final StudentService studentService;
  private final StudentHistoryService studentHistoryService;
  private final GradeService gradeService;
  private final GraduationEligibilityService graduationEligibilityService;

  @PostMapping("/students")
  @ResponseStatus(HttpStatus.CREATED)
  public StudentResponse createStudent(@RequestBody @Valid StudentCreateRequest request) {
    return studentService.createStudent(request);
  }

  @GetMapping("/students")
  public StudentPageResponse listStudents(
      @RequestParam(required = false) UUID cohortId,
      @RequestParam(required = false) UUID groupId,
      Pageable pageable) {
    return studentService.listStudents(cohortId, groupId, pageable);
  }

  @PatchMapping("/students/{studentId}")
  public StudentResponse updateStudent(
      @PathVariable UUID studentId, @RequestBody @Valid StudentUpdateRequest request) {
    return studentService.updateStudent(studentId, request);
  }

  @GetMapping("/students/{studentId}")
  public StudentResponse getStudent(@PathVariable UUID studentId) {
    return studentService.getStudent(studentId);
  }

  @GetMapping("/students/{studentId}/group-history")
  public List<StudentGroupHistoryResponse> listStudentGroupHistory(@PathVariable UUID studentId) {
    return studentHistoryService.listStudentGroupHistory(studentId);
  }

  @PostMapping("/students/{studentId}/group-history")
  @ResponseStatus(HttpStatus.CREATED)
  public StudentGroupHistoryResponse changeStudentGroup(
      @PathVariable UUID studentId, @RequestBody @Valid StudentGroupHistoryCreateRequest request) {
    return studentHistoryService.changeStudentGroup(studentId, request);
  }

  @GetMapping("/students/{studentId}/track-history")
  public List<StudentTrackHistoryResponse> listStudentTrackHistory(@PathVariable UUID studentId) {
    return studentHistoryService.listStudentTrackHistory(studentId);
  }

  @PostMapping("/students/{studentId}/track-history")
  @ResponseStatus(HttpStatus.CREATED)
  public StudentTrackHistoryResponse changeStudentTrack(
      @PathVariable UUID studentId, @RequestBody @Valid StudentTrackHistoryCreateRequest request) {
    return studentHistoryService.changeStudentTrack(studentId, request);
  }

  @GetMapping("/students/{studentId}/grades")
  public GradePageResponse listStudentGrades(
      @PathVariable UUID studentId,
      @RequestParam(required = false) UUID semesterId,
      Pageable pageable) {
    return gradeService.listStudentGrades(studentId, semesterId, pageable);
  }

  @GetMapping("/students/{studentId}/graduation-eligibility")
  public GraduationEligibilityResponse getGraduationEligibility(@PathVariable UUID studentId) {
    return graduationEligibilityService.getGraduationEligibility(studentId);
  }
}
