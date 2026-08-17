package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.GradeDisputeCreateRequest;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResolveRequest;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResponse;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.GradeDisputeMapper;
import app.mata.gradup.model.DisputeStatus;
import app.mata.gradup.repository.GradeDisputeRepository;
import app.mata.gradup.repository.GradeHistoryRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.model.JGradeDispute;
import app.mata.gradup.repository.model.JGradeHistory;
import app.mata.gradup.service.utils.Students;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GradeDisputeService {

  private final GradeDisputeRepository gradeDisputeRepository;
  private final GradeRepository gradeRepository;
  private final GradeHistoryRepository gradeHistoryRepository;
  private final StudentRepository studentRepository;
  private final GradeDisputeMapper gradeDisputeMapper;

  @Transactional
  public GradeDisputeResponse createGradeDispute(
      UUID gradeId, GradeDisputeCreateRequest request, UUID currentUserId) {
    var grade =
        gradeRepository
            .findById(gradeId)
            .orElseThrow(() -> new NotFoundException("Grade not found"));
    if (gradeDisputeRepository.findByGradeIdAndStatus(gradeId, DisputeStatus.PENDING).isPresent()) {
      throw new ConflictException("A pending dispute already exists for this grade");
    }
    var dispute =
        gradeDisputeRepository.save(
            JGradeDispute.builder()
                .grade(grade)
                .student(grade.getStudent())
                .reason(request.getReason())
                .status(DisputeStatus.PENDING)
                .build());
    return gradeDisputeMapper.toRest(dispute);
  }

  @Transactional
  public GradeDisputeResponse resolveDispute(
      UUID disputeId, GradeDisputeResolveRequest request, UUID currentUserId) {
    var dispute =
        gradeDisputeRepository
            .findById(disputeId)
            .orElseThrow(() -> new NotFoundException("Dispute not found"));
    if (dispute.getStatus() != DisputeStatus.PENDING) {
      throw new ConflictException("Dispute is not in PENDING status");
    }
    DisputeStatus requestedStatus = DisputeStatus.valueOf(request.getStatus().name());
    if (requestedStatus == DisputeStatus.PENDING) {
      throw new BusinessRuleException("A dispute cannot be resolved to PENDING");
    }
    UUID resultingHistoryId = null;
    if (requestedStatus == DisputeStatus.RESOLVED && request.getNewScore() != null) {
      var grade = dispute.getGrade();
      grade.setScore(BigDecimal.valueOf(request.getNewScore()));
      // The trigger archives the previous score with the current user as modified_by.
      grade.setRecordedBy(currentUserId);
      gradeRepository.save(grade);
      resultingHistoryId = latestHistoryId(grade.getId());
    }
    dispute.setStatus(requestedStatus);
    dispute.setResolvedAt(Instant.now());
    dispute.setResolvedBy(currentUserId);
    dispute.setResolutionNote(request.getResolutionNote());
    dispute.setResultingHistoryId(resultingHistoryId);
    return gradeDisputeMapper.toRest(gradeDisputeRepository.save(dispute));
  }

  @Transactional(readOnly = true)
  public List<GradeDisputeResponse> listStudentDisputes(UUID studentId) {
    Students.requireStudent(studentRepository, studentId);
    return gradeDisputeRepository.findByStudentId(studentId).stream()
        .map(gradeDisputeMapper::toRest)
        .toList();
  }

  private UUID latestHistoryId(UUID gradeId) {
    return gradeHistoryRepository.findByGradeId(gradeId).stream()
        .max(Comparator.comparing(JGradeHistory::getModifiedAt))
        .map(JGradeHistory::getId)
        .orElse(null);
  }
}
