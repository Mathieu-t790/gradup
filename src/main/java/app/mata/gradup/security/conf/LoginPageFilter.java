package app.mata.gradup.security.conf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;

public class LoginPageFilter extends DefaultLoginPageGeneratingFilter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    super.doFilter(new NormalizedQueryStringRequest((HttpServletRequest) request), response, chain);
  }

  private static final class NormalizedQueryStringRequest extends HttpServletRequestWrapper {

    private NormalizedQueryStringRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getQueryString() {
      String queryString = super.getQueryString();
      return (queryString == null || queryString.isEmpty()) ? null : queryString;
    }
  }
}
