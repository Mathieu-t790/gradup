package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryResponse;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.StudentMapper;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JStudentGroupHistory;
import app.mata.gradup.repository.model.JStudentTrackHistory;
import app.mata.gradup.service.utils.Students;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class StudentHistoryService {

  private final StudentRepository studentRepository;
  private final GroupRepository groupRepository;
  private final TrackRepository trackRepository;
  private final StudentGroupHistoryRepository studentGroupHistoryRepository;
  private final StudentTrackHistoryRepository studentTrackHistoryRepository;
  private final StudentMapper studentMapper;

  @Transactional(readOnly = true)
  public List<StudentGroupHistoryResponse> listStudentGroupHistory(UUID studentId) {
    requireStudent(studentId);
    return studentGroupHistoryRepository.findByStudentIdOrderByStartDateAsc(studentId).stream()
        .map(studentMapper::toDomain)
        .map(studentMapper::toRest)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<StudentTrackHistoryResponse> listStudentTrackHistory(UUID studentId) {
    requireStudent(studentId);
    return studentTrackHistoryRepository.findByStudentIdOrderByStartDateAsc(studentId).stream()
        .map(studentMapper::toDomain)
        .map(studentMapper::toRest)
        .toList();
  }

  @Transactional
  public StudentGroupHistoryResponse changeStudentGroup(
      UUID studentId, StudentGroupHistoryCreateRequest request) {
    var student = requireStudent(studentId);
    var group =
        groupRepository
            .findById(request.getGroupId())
            .orElseThrow(() -> new NotFoundException("Group not found"));
    if (!group.getCohort().getId().equals(student.getCohort().getId())) {
      throw new BusinessRuleException("Group does not belong to the student's cohort");
    }
    closeOpenGroupHistory(student, request.getStartDate().minusDays(1));
    var saved =
        studentGroupHistoryRepository.save(
            JStudentGroupHistory.builder()
                .student(student)
                .group(group)
                .startDate(request.getStartDate())
                .changeReason(studentMapper.nullableOrNull(request.getChangeReason_JsonNullable()))
                .build());
    return studentMapper.toRest(studentMapper.toDomain(saved));
  }

  @Transactional
  public StudentTrackHistoryResponse changeStudentTrack(
      UUID studentId, StudentTrackHistoryCreateRequest request) {
    var student = requireStudent(studentId);
    var track =
        trackRepository
            .findById(request.getTrackId())
            .orElseThrow(() -> new NotFoundException("Track not found"));
    closeOpenTrackHistory(student, request.getStartDate().minusDays(1));
    var saved =
        studentTrackHistoryRepository.save(
            JStudentTrackHistory.builder()
                .student(student)
                .track(track)
                .startDate(request.getStartDate())
                .changeReason(studentMapper.nullableOrNull(request.getChangeReason_JsonNullable()))
                .build());
    return studentMapper.toRest(studentMapper.toDomain(saved));
  }

  private void closeOpenGroupHistory(JStudent student, LocalDate endDate) {
    openGroupHistory(student).ifPresent(history -> history.setEndDate(endDate));
  }

  private void closeOpenTrackHistory(JStudent student, LocalDate endDate) {
    openTrackHistory(student).ifPresent(history -> history.setEndDate(endDate));
  }

  private Optional<JStudentGroupHistory> openGroupHistory(JStudent student) {
    return studentGroupHistoryRepository.findFirstByStudentIdAndEndDateIsNull(student.getId());
  }

  private Optional<JStudentTrackHistory> openTrackHistory(JStudent student) {
    return studentTrackHistoryRepository.findFirstByStudentIdAndEndDateIsNull(student.getId());
  }

  private JStudent requireStudent(UUID studentId) {
    return Students.requireStudent(studentRepository, studentId);
  }
}
