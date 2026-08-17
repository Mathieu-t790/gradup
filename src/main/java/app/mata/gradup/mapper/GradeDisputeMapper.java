package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.GradeDisputeResponse;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JGradeDispute;
import app.mata.gradup.service.utils.Users;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GradeDisputeMapper {

  private final UserRepository userRepository;

  public GradeDisputeResponse toRest(JGradeDispute dispute) {
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
        .status(app.mata.gradup.endpoint.rest.model.DisputeStatus.valueOf(dispute.getStatus().name()))
        .createdAt(dispute.getCreatedAt())
        .resolvedAt(dispute.getResolvedAt())
        .resolvedByName(resolvedByName(dispute.getResolvedBy()))
        .resolutionNote(dispute.getResolutionNote())
        .resultingHistoryId(dispute.getResultingHistoryId());
  }

  private String resolvedByName(UUID userId) {
    if (userId == null) {
      return null;
    }
    return userRepository.findById(userId).map(Users::fullName).orElse(null);
  }
}