package app.mata.gradup.security.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final int UNAUTHORIZED = 401;

  /** Browser UI paths: anonymous users are redirected to the login page instead of a 401 JSON. */
  private static final String[] WEB_PATH_PREFIXES = {"/promotions"};

  private final RestErrorWriter restErrorWriter;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    if (isWebPath(request.getRequestURI())) {
      response.sendRedirect("/login");
      return;
    }
    restErrorWriter.write(response, UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
  }

  private static boolean isWebPath(String uri) {
    return Arrays.stream(WEB_PATH_PREFIXES).anyMatch(uri::startsWith);
  }
}
