package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryResponse;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.StudentMapper;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.model.JStudentGroupHistory;
import app.mata.gradup.repository.model.JStudentTrackHistory;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class StudentHistoryService {

  private final StudentRepository studentRepository;
  private final StudentGroupHistoryRepository studentGroupHistoryRepository;
  private final StudentTrackHistoryRepository studentTrackHistoryRepository;
  private final StudentMapper studentMapper;

  @Transactional(readOnly = true)
  public List<StudentGroupHistoryResponse> listStudentGroupHistory(UUID studentId) {
    requireStudent(studentId);
    return studentGroupHistoryRepository.findByStudentIdOrderByStartDateDesc(studentId).stream()
        .sorted(Comparator.comparing(JStudentGroupHistory::getStartDate))
        .map(studentMapper::toDomain)
        .map(studentMapper::toRest)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<StudentTrackHistoryResponse> listStudentTrackHistory(UUID studentId) {
    requireStudent(studentId);
    return studentTrackHistoryRepository.findByStudentIdOrderByStartDateDesc(studentId).stream()
        .sorted(Comparator.comparing(JStudentTrackHistory::getStartDate))
        .map(studentMapper::toDomain)
        .map(studentMapper::toRest)
        .toList();
  }

  private void requireStudent(UUID studentId) {
    if (!studentRepository.existsById(studentId)) {
      throw new NotFoundException("Student not found");
    }
  }
}
