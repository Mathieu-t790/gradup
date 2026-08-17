package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.CourseOfferingMapper;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.TeacherRepository;
import app.mata.gradup.repository.model.JTeacherAssignment;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CourseOfferingService {

  private final CourseOfferingRepository courseOfferingRepository;
  private final TeacherAssignmentRepository teacherAssignmentRepository;
  private final TeacherRepository teacherRepository;
  private final ExamRepository examRepository;
  private final CourseOfferingMapper courseOfferingMapper;

  @Transactional(readOnly = true)
  public CourseOfferingResponse getCourseOffering(UUID offeringId) {
    var offering =
        courseOfferingRepository
            .findById(offeringId)
            .orElseThrow(() -> new NotFoundException("Course offering not found"));
    return courseOfferingMapper.toRest(
        offering, teacherAssignmentRepository.findByOfferingId(offering.getId()));
  }

  @Transactional
  public void assignTeacher(UUID offeringId, UUID teacherId) {
    var offering =
        courseOfferingRepository
            .findById(offeringId)
            .orElseThrow(() -> new NotFoundException("Course offering not found"));
    var teacher =
        teacherRepository
            .findById(teacherId)
            .orElseThrow(() -> new NotFoundException("Teacher not found"));
    if (teacherAssignmentRepository.existsByTeacherIdAndOfferingId(teacherId, offeringId)) {
      throw new ConflictException("Teacher is already assigned to this course offering");
    }
    teacherAssignmentRepository.save(
        JTeacherAssignment.builder().offering(offering).teacher(teacher).build());
  }

  @Transactional
  public void unassignTeacher(UUID offeringId, UUID teacherId) {
    courseOfferingRepository
        .findById(offeringId)
        .orElseThrow(() -> new NotFoundException("Course offering not found"));
    teacherRepository
        .findById(teacherId)
        .orElseThrow(() -> new NotFoundException("Teacher not found"));
    teacherAssignmentRepository.deleteByOfferingIdAndTeacherId(offeringId, teacherId);
  }

  @Transactional(readOnly = true)
  public List<ExamResponse> listOfferingExams(UUID offeringId) {
    courseOfferingRepository
        .findById(offeringId)
        .orElseThrow(() -> new NotFoundException("Course offering not found"));
    return examRepository.findByOfferingId(offeringId).stream()
        .map(courseOfferingMapper::toRest)
        .toList();
  }
}
