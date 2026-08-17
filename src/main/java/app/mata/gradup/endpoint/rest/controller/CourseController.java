package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.CourseCreateRequest;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
import app.mata.gradup.endpoint.rest.model.CourseUpdateRequest;
import app.mata.gradup.service.CourseService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class CourseController {

  private final CourseService courseService;

  @GetMapping("/courses")
  public List<CourseResponse> listCourses(
      @RequestParam(required = false) UUID trackId,
      @RequestParam(required = false) Integer semesterNumber) {
    return courseService.listCourses(trackId, semesterNumber);
  }

  @PostMapping("/courses")
  @ResponseStatus(HttpStatus.CREATED)
  public CourseResponse createCourse(@RequestBody @Valid CourseCreateRequest request) {
    return courseService.createCourse(request);
  }

  @GetMapping("/courses/{courseId}")
  public CourseResponse getCourse(@PathVariable UUID courseId) {
    return courseService.getCourse(courseId);
  }

  @PatchMapping("/courses/{courseId}")
  public CourseResponse updateCourse(
      @PathVariable UUID courseId, @RequestBody @Valid CourseUpdateRequest request) {
    return courseService.updateCourse(courseId, request);
  }
}
