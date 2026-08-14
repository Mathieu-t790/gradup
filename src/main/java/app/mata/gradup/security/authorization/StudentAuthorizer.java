package app.mata.gradup.security.authorization;

import app.mata.gradup.security.userDetails.JUserDetails;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public class StudentAuthorizer extends OwnerAuthorizer {

  @Override
  protected AuthorizationDecision checkOwnership(
      JUserDetails userDetails, RequestAuthorizationContext context) {
    var studentId = context.getVariables().get("studentId");
    return new AuthorizationDecision(
        studentId != null && studentId.equals(userDetails.userId().toString()));
  }
}
