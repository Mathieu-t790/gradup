package app.mata.gradup.security.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class WebAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final RestAuthenticationEntryPoint restEntryPoint;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    if (isBrowserRequest(request)) {
      response.sendRedirect("/login");
    } else {
      restEntryPoint.commence(request, response, authException);
    }
  }

  private boolean isBrowserRequest(HttpServletRequest request) {
    String accept = request.getHeader("Accept");
    return accept != null && accept.contains("text/html");
  }
}
