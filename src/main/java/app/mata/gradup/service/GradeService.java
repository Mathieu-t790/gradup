package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.GradeCreateRequest;
import app.mata.gradup.endpoint.rest.model.GradeHistoryEntryResponse;
import app.mata.gradup.endpoint.rest.model.GradePageResponse;
import app.mata.gradup.endpoint.rest.model.GradeResponse;
import app.mata.gradup.endpoint.rest.model.GradeUpdateRequest;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.GradeHistoryMapper;
import app.mata.gradup.mapper.GradeMapper;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GradeHistoryRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JGrade;
import app.mata.gradup.repository.model.JGradeHistory;
import app.mata.gradup.repository.model.JUser;
import app.mata.gradup.service.utils.Students;
import app.mata.gradup.service.utils.Users;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GradeService {

  private final GradeRepository gradeRepository;
  private final GradeHistoryRepository gradeHistoryRepository;
  private final ExamRepository examRepository;
  private final StudentRepository studentRepository;
  private final UserRepository userRepository;
  private final GradeMapper gradeMapper;
  private final GradeHistoryMapper gradeHistoryMapper;

  @Transactional
  public GradeResponse recordGrade(UUID examId, GradeCreateRequest request, UUID currentUserId) {
    var exam =
        examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Exam not found"));
    var student = Students.requireStudent(studentRepository, request.getStudentId());
    if (gradeRepository.findByStudentIdAndExamId(student.getId(), examId).isPresent()) {
      throw new ConflictException("A grade already exists for this student and exam");
    }
    var grade =
        gradeRepository.save(
            JGrade.builder()
                .student(student)
                .exam(exam)
                .score(BigDecimal.valueOf(request.getScore()))
                .recordedBy(currentUserId)
                .build());
    return toGradeResponse(grade);
  }

  @Transactional
  public GradeResponse updateGrade(UUID gradeId, GradeUpdateRequest request, UUID currentUserId) {
    var grade =
        gradeRepository
            .findById(gradeId)
            .orElseThrow(() -> new NotFoundException("Grade not found"));
    grade.setScore(BigDecimal.valueOf(request.getScore()));
    grade.setRecordedBy(currentUserId);
    gradeRepository.save(grade);
    // The DB trigger archives the previous score in grade_history; persist the reason here.
    gradeHistoryRepository.findByGradeId(gradeId).stream()
        .max(Comparator.comparing(JGradeHistory::getModifiedAt))
        .ifPresent(
            entry -> {
              entry.setReason(request.getReason());
              gradeHistoryRepository.save(entry);
            });
    return toGradeResponse(grade);
  }

  private GradeResponse toGradeResponse(JGrade grade) {
    return gradeMapper.toRest(
        gradeMapper.toDomain(
            grade,
            Users.fullName(grade.getStudent().getUser()),
            grade.getExam().getLabel(),
            grade.getExam().getOffering().getCourse().getReference(),
            userRepository.findById(grade.getRecordedBy()).map(Users::fullName).orElse(null)));
  }

  @Transactional(readOnly = true)
  public GradePageResponse listStudentGrades(UUID studentId, UUID semesterId, Pageable pageable) {
    Students.requireStudent(studentRepository, studentId);
    var page =
        semesterId == null
            ? gradeRepository.findByStudentId(studentId, pageable)
            : gradeRepository.findByStudentIdAndSemesterId(studentId, semesterId, pageable);
    var recordedNames = recordedNamesBy(page.getContent());
    return toGradePageResponse(
        page.map(
            grade ->
                gradeMapper.toRest(
                    gradeMapper.toDomain(
                        grade,
                        Users.fullName(grade.getStudent().getUser()),
                        grade.getExam().getLabel(),
                        grade.getExam().getOffering().getCourse().getReference(),
                        recordedNames.get(grade.getRecordedBy())))));
  }

  @Transactional(readOnly = true)
  public List<GradeResponse> listExamGrades(UUID examId) {
    examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Exam not found"));
    var grades = gradeRepository.findByExamId(examId);
    var recordedNames = recordedNamesBy(grades);
    return grades.stream()
        .sorted(
            Comparator.comparing(
                    (JGrade grade) -> grade.getStudent().getUser().getLastName(),
                    String.CASE_INSENSITIVE_ORDER)
                .thenComparing(
                    grade -> grade.getStudent().getUser().getFirstName(),
                    String.CASE_INSENSITIVE_ORDER))
        .map(
            grade ->
                gradeMapper.toRest(
                    gradeMapper.toDomain(
                        grade,
                        Users.fullName(grade.getStudent().getUser()),
                        grade.getExam().getLabel(),
                        grade.getExam().getOffering().getCourse().getReference(),
                        recordedNames.get(grade.getRecordedBy()))))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<GradeHistoryEntryResponse> listGradeHistory(UUID gradeId) {
    gradeRepository.findById(gradeId).orElseThrow(() -> new NotFoundException("Grade not found"));
    var entries = gradeHistoryRepository.findByGradeId(gradeId);
    var modifiedNames = modifiedNamesBy(entries);
    return entries.stream()
        .sorted(Comparator.comparing(JGradeHistory::getModifiedAt))
        .map(
            entry ->
                gradeHistoryMapper.toRest(
                    gradeHistoryMapper.toDomain(entry, modifiedNames.get(entry.getModifiedBy()))))
        .toList();
  }

  private Map<UUID, String> recordedNamesBy(List<JGrade> grades) {
    var userIds = grades.stream().map(JGrade::getRecordedBy).distinct().toList();
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(JUser::getId, Users::fullName));
  }

  private Map<UUID, String> modifiedNamesBy(List<JGradeHistory> entries) {
    var userIds = entries.stream().map(JGradeHistory::getModifiedBy).distinct().toList();
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(JUser::getId, Users::fullName));
  }

  private static GradePageResponse toGradePageResponse(Page<GradeResponse> page) {
    return new GradePageResponse()
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .first(page.isFirst())
        .last(page.isLast())
        .content(page.getContent());
  }
}
