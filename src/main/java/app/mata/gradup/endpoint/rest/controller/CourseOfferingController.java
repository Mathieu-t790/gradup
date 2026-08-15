package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.service.CourseOfferingService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class CourseOfferingController {

  private final CourseOfferingService courseOfferingService;

  @GetMapping("/course-offerings/{offeringId}")
  public CourseOfferingResponse getCourseOffering(@PathVariable UUID offeringId) {
    return courseOfferingService.getCourseOffering(offeringId);
  }
}
