package app.mata.gradup.security.conf;

import app.mata.gradup.security.error.WebAccessDeniedHandler;
import app.mata.gradup.security.error.WebAuthenticationEntryPoint;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConf {

  private final UserDetailsService userDetailsService;
  private final WebAuthenticationEntryPoint authenticationEntryPoint;
  private final WebAccessDeniedHandler accessDeniedHandler;
  private final RoutePolicy routePolicy;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.userDetailsService(userDetailsService)
        .authorizeHttpRequests(routePolicy::configure)
        .formLogin(form -> form.usernameParameter("email").defaultSuccessUrl("/", true))
        .logout(logout -> logout.logoutSuccessUrl("/login"))
        .csrf(csrf -> csrf.ignoringRequestMatchers(RoutePolicy.CSRF_IGNORED_PATHS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler));
    return http.build();
  }
}
