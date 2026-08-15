package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.TeacherCreateRequest;
import app.mata.gradup.endpoint.rest.model.TeacherResponse;
import app.mata.gradup.service.TeacherService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TeacherController {

  private final TeacherService teacherService;

  @GetMapping("/teachers")
  public List<TeacherResponse> listTeachers() {
    return teacherService.listTeachers();
  }

  @PostMapping("/teachers")
  @ResponseStatus(HttpStatus.CREATED)
  public TeacherResponse createTeacher(@Valid @RequestBody TeacherCreateRequest request) {
    return teacherService.createTeacher(request);
  }

  @GetMapping("/teachers/{teacherId}/course-offerings")
  public List<CourseOfferingResponse> listTeacherCourseOfferings(@PathVariable UUID teacherId) {
    return teacherService.listTeacherCourseOfferings(teacherId);
  }
}
