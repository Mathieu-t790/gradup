package app.mata.gradup.security.error;

import app.mata.gradup.endpoint.rest.model.Error;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RestErrorWriter {

  private final ObjectMapper objectMapper;

  public void write(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getOutputStream(), new Error().code(code).message(message));
  }
}
