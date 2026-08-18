package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.TeacherCreateRequest;
import app.mata.gradup.endpoint.rest.model.TeacherResponse;
import app.mata.gradup.exception.BadRequestException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mail.Email;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.mapper.CourseOfferingMapper;
import app.mata.gradup.mapper.TeacherMapper;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.TeacherRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JTeacher;
import app.mata.gradup.repository.model.JTeacherAssignment;
import app.mata.gradup.repository.model.JUser;
import app.mata.gradup.service.utils.EmailAssets;
import app.mata.gradup.service.utils.HtmlTemplater;
import app.mata.gradup.service.utils.Wording;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class TeacherService {

  private final TeacherRepository teacherRepository;
  private final TeacherAssignmentRepository teacherAssignmentRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TeacherMapper teacherMapper;
  private final CourseOfferingMapper courseOfferingMapper;
  private final Mailer mailer;
  private final HtmlTemplater htmlTemplater;

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
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new ConflictException("A user with email " + request.getEmail() + " already exists");
    }

    var initialPassword = randomInitialPassword();
    var user =
        JUser.builder()
            .lastName(request.getLastName())
            .firstName(request.getFirstName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(initialPassword))
            .role(Role.TEACHER)
            .isActive(true)
            .build();
    JUser savedUser;
    try {
      savedUser = userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("A user with email " + request.getEmail() + " already exists");
    }

    var teacher =
        JTeacher.builder()
            .user(savedUser)
            .specialty(teacherMapper.nullableOrNull(request.getSpecialty_JsonNullable()))
            .build();
    var savedTeacher = teacherRepository.save(teacher);

    sendCredentials(savedUser, initialPassword);

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
}
