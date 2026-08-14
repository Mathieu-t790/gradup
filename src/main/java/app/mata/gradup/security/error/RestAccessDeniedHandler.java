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
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private static final int FORBIDDEN = 403;

  private final RestErrorWriter restErrorWriter;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    restErrorWriter.write(response, FORBIDDEN, "FORBIDDEN", "Access denied");
  }
}
