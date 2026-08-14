package app.mata.gradup.security.authorization;

import app.mata.gradup.security.userDetails.JUserDetails;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public final class AuthorizationUtils {

  private AuthorizationUtils() {}

  public static boolean hasRole(Authentication auth, String role) {
    return auth != null
        && auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
  }

  public static Optional<JUserDetails> userDetails(Authentication auth) {
    if (auth == null || !(auth.getPrincipal() instanceof JUserDetails userDetails)) {
      return Optional.empty();
    }
    return Optional.of(userDetails);
  }

  public static UUID toUuid(String raw) {
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
