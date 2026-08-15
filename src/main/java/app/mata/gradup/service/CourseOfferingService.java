package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.CourseOfferingMapper;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CourseOfferingService {

  private final CourseOfferingRepository courseOfferingRepository;
  private final TeacherAssignmentRepository teacherAssignmentRepository;
  private final CourseOfferingMapper courseOfferingMapper;

  @Transactional(readOnly = true)
  public CourseOfferingResponse getCourseOffering(java.util.UUID offeringId) {
    var offering =
        courseOfferingRepository
            .findById(offeringId)
            .orElseThrow(() -> new NotFoundException("Course offering not found"));
    return courseOfferingMapper.toRest(
        offering, teacherAssignmentRepository.findByOfferingId(offering.getId()));
  }
}
