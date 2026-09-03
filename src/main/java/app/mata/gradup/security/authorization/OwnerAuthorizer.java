package app.mata.gradup.security.authorization;

import app.mata.gradup.model.Role;
import app.mata.gradup.security.userDetails.JUserDetails;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.transaction.annotation.Transactional;

public abstract class OwnerAuthorizer implements AuthorizationManager<RequestAuthorizationContext> {

  @Override
  @Transactional(readOnly = true)
  public AuthorizationDecision check(
      Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    var auth = authentication.get();
    if (AuthorizationUtils.hasRole(auth, Role.ADMIN)) {
      return new AuthorizationDecision(true);
    }
    var userDetails = AuthorizationUtils.userDetails(auth);
    if (!AuthorizationUtils.hasRole(auth, Role.STUDENT) || userDetails.isEmpty()) {
      return new AuthorizationDecision(false);
    }
    return checkOwnership(userDetails.get(), context);
  }

  protected abstract AuthorizationDecision checkOwnership(
      JUserDetails userDetails, RequestAuthorizationContext context);
}
