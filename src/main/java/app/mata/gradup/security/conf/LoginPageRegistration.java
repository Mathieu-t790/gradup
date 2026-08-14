package app.mata.gradup.security.conf;

import java.util.Map;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

@Component
public class LoginPageRegistration {

  public void register(HttpSecurity http) {
    DefaultLoginPageGeneratingFilter loginPageGeneratingFilter =
        new DefaultLoginPageGeneratingFilter();
    loginPageGeneratingFilter.setResolveHiddenInputs(
        request -> {
          CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
          return csrfToken != null
              ? Map.of(csrfToken.getParameterName(), csrfToken.getToken())
              : Map.of();
        });
    http.setSharedObject(DefaultLoginPageGeneratingFilter.class, loginPageGeneratingFilter);
    http.addFilter(loginPageGeneratingFilter);
    http.removeConfigurer(DefaultLoginPageConfigurer.class);
  }
}
