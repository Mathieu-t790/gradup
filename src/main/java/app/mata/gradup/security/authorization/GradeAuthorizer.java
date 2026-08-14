package app.mata.gradup.security.authorization;

import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.security.userDetails.JUserDetails;
import lombok.AllArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GradeAuthorizer extends OwnerAuthorizer {

  private final GradeRepository gradeRepository;

  @Override
  protected AuthorizationDecision checkOwnership(
      JUserDetails userDetails, RequestAuthorizationContext context) {
    var gradeId = AuthorizationUtils.toUuid(context.getVariables().get("gradeId"));
    if (gradeId == null) {
      return new AuthorizationDecision(false);
    }
    var grade = gradeRepository.findById(gradeId).orElse(null);
    boolean owner = grade != null && grade.getStudent().getId().equals(userDetails.userId());
    return new AuthorizationDecision(owner);
  }
}
