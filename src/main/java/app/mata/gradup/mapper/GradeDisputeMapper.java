package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.DisputeStatus;
import app.mata.gradup.endpoint.rest.model.GradeDisputeResponse;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JGradeDispute;
import app.mata.gradup.service.utils.Users;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GradeDisputeMapper {

  private final UserRepository userRepository;

  public GradeDisputeResponse toRest(JGradeDispute dispute) {
    return toRest(dispute, null);
  }

  public GradeDisputeResponse toRest(JGradeDispute dispute, Map<UUID, String> resolvedNamesById) {
    var grade = dispute.getGrade();
    var exam = grade.getExam();
    return new GradeDisputeResponse()
        .id(dispute.getId())
        .gradeId(grade.getId())
        .studentId(dispute.getStudent().getId())
        .studentName(Users.fullName(dispute.getStudent().getUser()))
        .courseReference(exam.getOffering().getCourse().getReference())
        .examLabel(exam.getLabel())
        .reason(dispute.getReason())
        .status(DisputeStatus.valueOf(dispute.getStatus().name()))
        .createdAt(dispute.getCreatedAt())
        .resolvedAt(dispute.getResolvedAt())
        .resolvedByName(resolvedName(dispute.getResolvedBy(), resolvedNamesById))
        .resolutionNote(dispute.getResolutionNote())
        .resultingHistoryId(dispute.getResultingHistoryId());
  }

  public app.mata.gradup.model.DisputeStatus toDomain(DisputeStatus status) {
    return status == null ? null : app.mata.gradup.model.DisputeStatus.valueOf(status.name());
  }

  private String resolvedName(UUID userId, Map<UUID, String> resolvedNamesById) {
    if (userId == null) {
      return null;
    }
    if (resolvedNamesById != null) {
      return resolvedNamesById.get(userId);
    }
    return userRepository.findById(userId).map(Users::fullName).orElse(null);
  }
}
