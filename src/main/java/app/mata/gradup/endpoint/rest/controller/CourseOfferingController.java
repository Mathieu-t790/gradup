package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.CourseOfferingCreateRequest;
import app.mata.gradup.endpoint.rest.model.CourseOfferingPageResponse;
import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.service.CourseOfferingService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class CourseOfferingController {

  private final CourseOfferingService courseOfferingService;

  @GetMapping("/course-offerings")
  public CourseOfferingPageResponse listCourseOfferings(
      @RequestParam(required = false) UUID semesterId,
      @RequestParam(required = false) UUID groupId,
      @RequestParam(required = false) UUID courseId,
      Pageable pageable) {
    return courseOfferingService.listCourseOfferings(semesterId, groupId, courseId, pageable);
  }

  @PostMapping("/course-offerings")
  @ResponseStatus(HttpStatus.CREATED)
  public CourseOfferingResponse createCourseOffering(
      @RequestBody CourseOfferingCreateRequest request) {
    return courseOfferingService.createCourseOffering(request);
  }

  @GetMapping("/course-offerings/{offeringId}")
  public CourseOfferingResponse getCourseOffering(@PathVariable UUID offeringId) {
    return courseOfferingService.getCourseOffering(offeringId);
  }

  @PostMapping("/course-offerings/{offeringId}/teachers/{teacherId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void assignTeacher(@PathVariable UUID offeringId, @PathVariable UUID teacherId) {
    courseOfferingService.assignTeacher(offeringId, teacherId);
  }

  @DeleteMapping("/course-offerings/{offeringId}/teachers/{teacherId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unassignTeacher(@PathVariable UUID offeringId, @PathVariable UUID teacherId) {
    courseOfferingService.unassignTeacher(offeringId, teacherId);
  }

  @GetMapping("/course-offerings/{offeringId}/exams")
  public List<ExamResponse> listOfferingExams(@PathVariable UUID offeringId) {
    return courseOfferingService.listOfferingExams(offeringId);
  }
}
