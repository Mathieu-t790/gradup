package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.GradeDisputeCreateRequest;
import app.mata.gradup.endpoint.rest.model.GradeDisputePageResponse;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResolveRequest;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResponse;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.GradeDisputeMapper;
import app.mata.gradup.model.DisputeStatus;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.GradeDisputeRepository;
import app.mata.gradup.repository.GradeHistoryRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JGradeDispute;
import app.mata.gradup.repository.model.JGradeHistory;
import app.mata.gradup.repository.model.JUser;
import app.mata.gradup.service.utils.Students;
import app.mata.gradup.service.utils.Users;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GradeDisputeService {

  private final GradeDisputeRepository gradeDisputeRepository;
  private final GradeRepository gradeRepository;
  private final GradeHistoryRepository gradeHistoryRepository;
  private final StudentRepository studentRepository;
  private final TeacherAssignmentRepository teacherAssignmentRepository;
  private final UserRepository userRepository;
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
    DisputeStatus requestedStatus = gradeDisputeMapper.toDomain(request.getStatus());
    if (requestedStatus == DisputeStatus.PENDING) {
      throw new BusinessRuleException("A dispute cannot be resolved to PENDING");
    }
    UUID resultingHistoryId = null;
    if (requestedStatus == DisputeStatus.RESOLVED && request.getNewScore() != null) {
      var grade = dispute.getGrade();
      grade.setScore(BigDecimal.valueOf(request.getNewScore()));
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

  @Transactional(readOnly = true)
  public GradeDisputePageResponse listDisputes(
      DisputeStatus status, Pageable pageable, UUID currentUserId, Role currentRole) {
    DisputeStatus effectiveStatus = status == null ? DisputeStatus.PENDING : status;
    Page<JGradeDispute> page;
    if (currentRole == Role.ADMIN) {
      page = gradeDisputeRepository.findByStatus(effectiveStatus, pageable);
    } else if (currentRole == Role.TEACHER) {
      var offeringIds =
          teacherAssignmentRepository.findByTeacherId(currentUserId).stream()
              .map(assignment -> assignment.getOffering().getId())
              .toList();
      page =
          offeringIds.isEmpty()
              ? Page.empty(pageable)
              : gradeDisputeRepository.findByStatusAndOfferingIds(
                  effectiveStatus, offeringIds, pageable);
    } else {
      throw new AccessDeniedException("Only ADMIN or TEACHER can list disputes");
    }
    return toPageResponse(page);
  }

  private GradeDisputePageResponse toPageResponse(Page<JGradeDispute> page) {
    Map<UUID, String> resolvedNamesById = resolvedNamesById(page);
    return new GradeDisputePageResponse()
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .first(page.isFirst())
        .last(page.isLast())
        .content(
            page.getContent().stream()
                .map(dispute -> gradeDisputeMapper.toRest(dispute, resolvedNamesById))
                .toList());
  }

  private Map<UUID, String> resolvedNamesById(Page<JGradeDispute> page) {
    var resolvedByIds =
        page.getContent().stream()
            .map(JGradeDispute::getResolvedBy)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (resolvedByIds.isEmpty()) {
      return Map.of();
    }
    return userRepository.findAllById(resolvedByIds).stream()
        .collect(Collectors.toMap(JUser::getId, Users::fullName));
  }

  private UUID latestHistoryId(UUID gradeId) {
    return gradeHistoryRepository.findByGradeId(gradeId).stream()
        .max(Comparator.comparing(JGradeHistory::getModifiedAt))
        .map(JGradeHistory::getId)
        .orElse(null);
  }
}
