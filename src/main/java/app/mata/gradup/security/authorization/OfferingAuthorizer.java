package app.mata.gradup.security.authorization;

import app.mata.gradup.model.Role;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GradeDisputeRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class OfferingAuthorizer implements AuthorizationManager<RequestAuthorizationContext> {

  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;
  private final GradeDisputeRepository gradeDisputeRepository;
  private final TeacherAssignmentRepository teacherAssignmentRepository;

  @Override
  @Transactional(readOnly = true)
  public AuthorizationDecision check(
      Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    var auth = authentication.get();
    if (AuthorizationUtils.hasRole(auth, Role.ADMIN)) {
      return new AuthorizationDecision(true);
    }
    var userDetails = AuthorizationUtils.userDetails(auth);
    if (!AuthorizationUtils.hasRole(auth, Role.TEACHER) || userDetails.isEmpty()) {
      return new AuthorizationDecision(false);
    }
    var offeringId = resolveOfferingId(context.getVariables());
    if (offeringId == null) {
      return new AuthorizationDecision(false);
    }
    boolean assigned =
        teacherAssignmentRepository.existsByTeacherIdAndOfferingId(
            userDetails.get().userId(), offeringId);
    return new AuthorizationDecision(assigned);
  }

  private UUID resolveOfferingId(Map<String, String> variables) {
    if (variables.containsKey("offeringId")) {
      return AuthorizationUtils.toUuid(variables.get("offeringId"));
    }
    if (variables.containsKey("examId")) {
      var examId = AuthorizationUtils.toUuid(variables.get("examId"));
      return examId == null
          ? null
          : examRepository.findById(examId).map(exam -> exam.getOffering().getId()).orElse(null);
    }
    if (variables.containsKey("gradeId")) {
      var gradeId = AuthorizationUtils.toUuid(variables.get("gradeId"));
      return gradeId == null
          ? null
          : gradeRepository
              .findById(gradeId)
              .map(grade -> grade.getExam().getOffering().getId())
              .orElse(null);
    }
    if (variables.containsKey("disputeId")) {
      var disputeId = AuthorizationUtils.toUuid(variables.get("disputeId"));
      return disputeId == null
          ? null
          : gradeDisputeRepository
              .findById(disputeId)
              .map(dispute -> dispute.getGrade().getExam().getOffering().getId())
              .orElse(null);
    }
    return null;
  }
}
