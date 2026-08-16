package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.ExamMapper;
import app.mata.gradup.repository.ExamRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ExamService {

  private final ExamRepository examRepository;
  private final ExamMapper examMapper;

  @Transactional(readOnly = true)
  public ExamResponse getExam(UUID examId) {
    var exam =
        examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Exam not found"));
    return examMapper.toRest(exam);
  }
}
