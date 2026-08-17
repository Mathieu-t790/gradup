package app.mata.gradup.security.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class WebAccessDeniedHandler implements AccessDeniedHandler {

  private final RestAccessDeniedHandler restHandler;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    if (isBrowserRequest(request)) {
      response.sendRedirect("/access-denied");
    } else {
      restHandler.handle(request, response, accessDeniedException);
    }
  }

  private boolean isBrowserRequest(HttpServletRequest request) {
    String accept = request.getHeader("Accept");
    return accept != null && accept.contains("text/html");
  }
}
