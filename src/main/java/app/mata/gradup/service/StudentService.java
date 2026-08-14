package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.StudentCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentResponse;
import app.mata.gradup.endpoint.rest.model.StudentUpdateRequest;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.StudentMapper;
import app.mata.gradup.model.Group;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.Track;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JStudentGroupHistory;
import app.mata.gradup.repository.model.JUser;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class StudentService {

  private static final String DEFAULT_STUDENT_PASSWORD = "password";

  private final StudentRepository studentRepository;
  private final UserRepository userRepository;
  private final CohortRepository cohortRepository;
  private final GroupRepository groupRepository;
  private final StudentGroupHistoryRepository studentGroupHistoryRepository;
  private final StudentTrackHistoryRepository studentTrackHistoryRepository;
  private final StudentMapper studentMapper;

  @Transactional
  public StudentResponse createStudent(StudentCreateRequest request) {
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new ConflictException("A user with email " + request.getEmail() + " already exists");
    }
    var cohort =
        cohortRepository
            .findById(request.getCohortId())
            .orElseThrow(() -> new NotFoundException("Cohort not found"));
    var initialGroup =
        groupRepository
            .findById(request.getInitialGroupId())
            .orElseThrow(() -> new NotFoundException("Group not found"));
    if (!initialGroup.getCohort().getId().equals(cohort.getId())) {
      throw new BusinessRuleException("Initial group does not belong to the given cohort");
    }

    var user =
        JUser.builder()
            .lastName(request.getLastName())
            .firstName(request.getFirstName())
            .email(request.getEmail())
            .passwordHash(DEFAULT_STUDENT_PASSWORD)
            .role(Role.STUDENT)
            .isActive(true)
            .build();
    var savedUser = userRepository.save(user);

    var student =
        JStudent.builder()
            .user(savedUser)
            .cohort(cohort)
            .dateOfBirth(request.getDateOfBirth())
            .build();
    var savedStudent = studentRepository.save(student);

    var initialHistory =
        JStudentGroupHistory.builder()
            .student(savedStudent)
            .group(initialGroup)
            .startDate(LocalDate.now())
            .build();
    studentGroupHistoryRepository.save(initialHistory);

    return toResponse(savedStudent);
  }

  @Transactional
  public StudentResponse updateStudent(UUID studentId, StudentUpdateRequest request) {
    var student = findStudent(studentId);
    var user = student.getUser();

    if (request.getLastName() != null) {
      user.setLastName(request.getLastName());
    }
    if (request.getFirstName() != null) {
      user.setFirstName(request.getFirstName());
    }
    if (request.getEmail() != null) {
      updateEmail(user, request.getEmail());
    }
    if (request.getDateOfBirth_JsonNullable().isPresent()) {
      student.setDateOfBirth(request.getDateOfBirth());
    }
    if (request.getIsActive() != null) {
      user.setIsActive(request.getIsActive());
    }

    return toResponse(student);
  }

  private void updateEmail(JUser user, String email) {
    if (!email.equals(user.getEmail()) && userRepository.findByEmail(email).isPresent()) {
      throw new ConflictException("A user with email " + email + " already exists");
    }
    user.setEmail(email);
  }

  private JStudent findStudent(UUID studentId) {
    return studentRepository
        .findById(studentId)
        .orElseThrow(() -> new NotFoundException("Student not found"));
  }

  private StudentResponse toResponse(JStudent student) {
    return studentMapper.toRest(
        studentMapper.toDomain(student, currentGroup(student), currentTrack(student)));
  }

  private Group currentGroup(JStudent student) {
    return openGroupHistory(student)
        .map(JStudentGroupHistory::getGroup)
        .map(studentMapper::toGroup)
        .orElse(null);
  }

  private Track currentTrack(JStudent student) {
    return studentTrackHistoryRepository
        .findByStudentIdOrderByStartDateDesc(student.getId())
        .stream()
        .filter(history -> history.getEndDate() == null)
        .findFirst()
        .map(history -> studentMapper.toTrack(history.getTrack()))
        .orElse(null);
  }

  private java.util.Optional<JStudentGroupHistory> openGroupHistory(JStudent student) {
    return studentGroupHistoryRepository
        .findByStudentIdOrderByStartDateDesc(student.getId())
        .stream()
        .filter(history -> history.getEndDate() == null)
        .findFirst();
  }
}
