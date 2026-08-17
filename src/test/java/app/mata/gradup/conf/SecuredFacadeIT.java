package app.mata.gradup.conf;

import app.mata.gradup.model.Role;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JUser;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;

public abstract class SecuredFacadeIT extends FacadeIT {

  protected static final String TEST_PASSWORD = "my-cute-password";
  protected static final String ADMIN_EMAIL = "admin@cu.te";

  @Autowired protected UserRepository userRepository;
  @Autowired protected PasswordEncoder passwordEncoder;

  protected void useCookieAwareClient(TestRestTemplate restTemplate) {
    var httpClient = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory(httpClient));
  }

  protected void seedUser(String email, Role role) {
    if (userRepository.findByEmail(email).isEmpty()) {
      userRepository.save(
          JUser.builder()
              .lastName("Mathieu")
              .firstName("Tafita")
              .email(email)
              .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
              .role(role)
              .isActive(true)
              .build());
    }
  }

  protected void loginAsAdmin(TestRestTemplate restTemplate) {
    seedUser(ADMIN_EMAIL, Role.ADMIN);
    loginAs(restTemplate, ADMIN_EMAIL);
  }

  /**
   * Resets the user password to {@link #TEST_PASSWORD} (seed users are stored with a hash) then
   * logs in.
   */
  protected void loginAsUser(TestRestTemplate restTemplate, String email) {
    var user = userRepository.findByEmail(email).orElseThrow();
    user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
    userRepository.save(user);
    loginAs(restTemplate, email);
  }

  protected void loginAs(TestRestTemplate restTemplate, String email) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("email", email);
    formData.add("password", TEST_PASSWORD);
    formData.add("_csrf", csrfToken(restTemplate));
    restTemplate.postForEntity("/login", new HttpEntity<>(formData, headers), String.class);
  }

  protected static String csrfToken(TestRestTemplate restTemplate) {
    var loginPage = restTemplate.getForEntity("/login", String.class);
    assert loginPage.getBody() != null;
    var matcher =
        Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(loginPage.getBody());
    return matcher.find() ? matcher.group(1) : null;
  }
}
