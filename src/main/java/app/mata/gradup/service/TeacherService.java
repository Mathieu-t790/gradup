package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.TeacherCreateRequest;
import app.mata.gradup.endpoint.rest.model.TeacherResponse;
import app.mata.gradup.exception.BadRequestException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.CourseOfferingMapper;
import app.mata.gradup.mapper.TeacherMapper;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.TeacherRepository;
import app.mata.gradup.repository.model.JTeacher;
import app.mata.gradup.repository.model.JTeacherAssignment;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TeacherService {

  private final TeacherRepository teacherRepository;
  private final TeacherAssignmentRepository teacherAssignmentRepository;
  private final TeacherMapper teacherMapper;
  private final CourseOfferingMapper courseOfferingMapper;
  private final UserService userService;

  @Transactional(readOnly = true)
  public List<TeacherResponse> listTeachers() {
    return teacherRepository.findAll().stream()
        .map(teacherMapper::toDomain)
        .map(teacherMapper::toRest)
        .toList();
  }

  @Transactional
  public TeacherResponse createTeacher(TeacherCreateRequest request) {
    if (request.getLastName() == null || request.getLastName().isBlank()) {
      throw new BadRequestException("Teacher last name must not be blank");
    }
    if (request.getFirstName() == null || request.getFirstName().isBlank()) {
      throw new BadRequestException("Teacher first name must not be blank");
    }
    if (request.getEmail() == null || request.getEmail().isBlank()) {
      throw new BadRequestException("Teacher email must not be blank");
    }

    var created =
        userService.createUserWithRole(
            request.getLastName(), request.getFirstName(), request.getEmail(), Role.TEACHER);

    var teacher =
        JTeacher.builder()
            .user(created.user())
            .specialty(teacherMapper.nullableOrNull(request.getSpecialty_JsonNullable()))
            .build();
    var savedTeacher = teacherRepository.save(teacher);

    userService.sendCredentials(created.user(), created.initialPassword());

    return teacherMapper.toRest(teacherMapper.toDomain(savedTeacher));
  }

  @Transactional(readOnly = true)
  public List<CourseOfferingResponse> listTeacherCourseOfferings(UUID teacherId) {
    teacherRepository
        .findById(teacherId)
        .orElseThrow(() -> new NotFoundException("Teacher not found"));
    var teacherAssignments = teacherAssignmentRepository.findByTeacherId(teacherId);
    var offeringIds =
        teacherAssignments.stream().map(a -> a.getOffering().getId()).distinct().toList();
    var allAssignments = teacherAssignmentRepository.findByOfferingIdIn(offeringIds);
    var grouped =
        allAssignments.stream()
            .collect(java.util.stream.Collectors.groupingBy(a -> a.getOffering().getId()));
    return teacherAssignments.stream()
        .map(JTeacherAssignment::getOffering)
        .distinct()
        .map(offering -> courseOfferingMapper.toRest(offering, grouped.get(offering.getId())))
        .toList();
  }
}
