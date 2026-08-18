package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.StudentCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentPageResponse;
import app.mata.gradup.endpoint.rest.model.StudentResponse;
import app.mata.gradup.endpoint.rest.model.StudentSummaryResponse;
import app.mata.gradup.endpoint.rest.model.StudentUpdateRequest;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mail.Email;
import app.mata.gradup.mail.Mailer;
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
import app.mata.gradup.service.utils.EmailAssets;
import app.mata.gradup.service.utils.HtmlTemplater;
import app.mata.gradup.service.utils.Students;
import app.mata.gradup.service.utils.Wording;
import jakarta.mail.internet.InternetAddress;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class StudentService {

  private final StudentRepository studentRepository;
  private final UserRepository userRepository;
  private final CohortRepository cohortRepository;
  private final GroupRepository groupRepository;
  private final StudentGroupHistoryRepository studentGroupHistoryRepository;
  private final StudentTrackHistoryRepository studentTrackHistoryRepository;
  private final PasswordEncoder passwordEncoder;
  private final StudentMapper studentMapper;
  private final Mailer mailer;
  private final HtmlTemplater htmlTemplater;

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

    var initialPassword = randomInitialPassword();
    var user =
        JUser.builder()
            .lastName(request.getLastName())
            .firstName(request.getFirstName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(initialPassword))
            .role(Role.STUDENT)
            .isActive(true)
            .build();
    JUser savedUser;
    try {
      savedUser = userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("A user with email " + request.getEmail() + " already exists");
    }

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

    sendCredentials(savedUser, initialPassword);

    return toResponse(savedStudent);
  }

  @Transactional(readOnly = true)
  public StudentResponse getStudent(UUID studentId) {
    return toResponse(findStudent(studentId));
  }

  @Transactional(readOnly = true)
  public Page<JStudent> listForWeb(Pageable pageable) {
    return studentRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public JStudent getForWeb(UUID studentId) {
    return findStudent(studentId);
  }

  @Transactional(readOnly = true)
  public List<JStudentGroupHistory> groupHistoryForWeb(UUID studentId) {
    return studentGroupHistoryRepository.findByStudentIdOrderByStartDateDesc(studentId);
  }

  @Transactional(readOnly = true)
  public StudentPageResponse listStudents(UUID cohortId, UUID groupId, Pageable pageable) {
    var page = findStudents(cohortId, groupId, pageable);
    var students = page.getContent();
    var currentGroups = currentGroupsByStudent(students);
    var currentTracks = currentTracksByStudent(students);
    var summaryPage =
        page.map(
            student ->
                studentMapper.toRestSummary(
                    student,
                    currentGroups.get(student.getId()),
                    currentTracks.get(student.getId())));
    return toStudentPageResponse(summaryPage);
  }

  private Page<JStudent> findStudents(UUID cohortId, UUID groupId, Pageable pageable) {
    if (cohortId != null && groupId != null) {
      return studentRepository.findByCohortIdAndCurrentGroupId(cohortId, groupId, pageable);
    }
    if (groupId != null) {
      return studentRepository.findByCurrentGroupId(groupId, pageable);
    }
    if (cohortId != null) {
      return studentRepository.findByCohortId(cohortId, pageable);
    }
    return studentRepository.findAll(pageable);
  }

  private Map<UUID, Group> currentGroupsByStudent(List<JStudent> students) {
    var studentIds = students.stream().map(JStudent::getId).toList();
    if (studentIds.isEmpty()) {
      return Map.of();
    }
    return studentGroupHistoryRepository.findByStudentIdInAndEndDateIsNull(studentIds).stream()
        .collect(
            Collectors.toMap(
                history -> history.getStudent().getId(),
                history -> studentMapper.toGroup(history.getGroup()),
                (first, second) -> first));
  }

  private Map<UUID, Track> currentTracksByStudent(List<JStudent> students) {
    var studentIds = students.stream().map(JStudent::getId).toList();
    if (studentIds.isEmpty()) {
      return Map.of();
    }
    return studentTrackHistoryRepository.findByStudentIdInAndEndDateIsNull(studentIds).stream()
        .collect(
            Collectors.toMap(
                history -> history.getStudent().getId(),
                history -> studentMapper.toTrack(history.getTrack()),
                (first, second) -> first));
  }

  private static StudentPageResponse toStudentPageResponse(Page<StudentSummaryResponse> page) {
    return new StudentPageResponse()
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .first(page.isFirst())
        .last(page.isLast())
        .content(page.getContent());
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
      student.setDateOfBirth(studentMapper.nullableOrNull(request.getDateOfBirth_JsonNullable()));
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

  private static String randomInitialPassword() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private void sendCredentials(JUser user, String initialPassword) {
    Context context = new Context();
    context.setVariable("logoDataUri", EmailAssets.LOGO_DATA_URI);
    context.setVariable("signatureDataUri", EmailAssets.SIGNATURE_DATA_URI);
    context.setVariable("firstName", user.getFirstName());
    context.setVariable("email", user.getEmail());
    context.setVariable("password", initialPassword);
    String subject = Wording.get("credentials.subject", user.getReference());
    String htmlBody = htmlTemplater.render("email/credentials", context);
    try {
      mailer.accept(
          new Email(
              new InternetAddress(user.getEmail()),
              List.of(),
              List.of(),
              subject,
              htmlBody,
              List.of()));
    } catch (Exception e) {
      throw new RuntimeException("Could not send credentials for user " + user.getEmail(), e);
    }
  }

  private JStudent findStudent(UUID studentId) {
    return Students.requireStudent(studentRepository, studentId);
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
        .findFirstByStudentIdAndEndDateIsNull(student.getId())
        .map(history -> studentMapper.toTrack(history.getTrack()))
        .orElse(null);
  }

  private Optional<JStudentGroupHistory> openGroupHistory(JStudent student) {
    return studentGroupHistoryRepository.findFirstByStudentIdAndEndDateIsNull(student.getId());
  }
}
