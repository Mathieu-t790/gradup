package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.model.JCohort;
import java.net.URI;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

class PromotionWebIT extends SecuredFacadeIT {

  private static final Pattern CSRF_PATTERN =
      Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

  @MockBean private BucketComponent bucketComponent;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @BeforeEach
  void setUp() throws Exception {
    reset(bucketComponent);
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    when(bucketComponent.presign(any(), any()))
        .thenReturn(URI.create("http://localhost/diplomas.xlsx").toURL());
  }

  private String csrfFrom(ResponseEntity<String> page) {
    var body = page.getBody();
    if (body != null) {
      var matcher = CSRF_PATTERN.matcher(body);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    return csrfToken(restTemplate);
  }

  private ResponseEntity<String> postDiplomas(JCohort cohort, String action, String track) {
    var csrf = csrfFrom(restTemplate.getForEntity("/promotions", String.class));
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("action", action);
    formData.add("_csrf", csrf);
    if (track != null) {
      formData.add("track", track);
    }
    return restTemplate.postForEntity(
        "/promotions/" + cohort.getId() + "/diplomas",
        new HttpEntity<>(formData, headers),
        String.class);
  }

  @Test
  void anonymous_home_shows_landing() {
    var response = restTemplate.getForEntity("/", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("Gradup"));
    assertTrue(response.getBody().contains("Se connecter"));
  }

  @Test
  void anonymous_promotions_redirect_to_login() {
    var response = restTemplate.getForEntity("/promotions", String.class);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertNotNull(response.getHeaders().getLocation());
    assertTrue(response.getHeaders().getLocation().toString().contains("/login"));
  }

  @Test
  void authenticated_home_redirects_to_promotions() {
    loginAsAdmin(restTemplate);

    var response = restTemplate.getForEntity("/", String.class);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertTrue(response.getHeaders().getLocation().toString().contains("/promotions"));
  }

  @Test
  void admin_sees_promotions_page_with_counts_and_actions() {
    loginAsAdmin(restTemplate);
    seeder.cohort("Mpamakilay", 2021, 2024);

    var response = restTemplate.getForEntity("/promotions", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("Mpamakilay"));
    assertTrue(response.getBody().contains("Générer les diplômes"));
    assertTrue(response.getBody().contains("Télécharger les diplômés"));
    assertTrue(response.getBody().contains("EL"));
    assertTrue(response.getBody().contains("TN"));
    assertTrue(response.getBody().contains("Promotions"));
    assertTrue(response.getBody().contains("Diplômés"));
  }

  @Test
  void generate_action_redirects_back_to_promotions() {
    loginAsAdmin(restTemplate);
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);

    var response = postDiplomas(cohort, "generate", null);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertTrue(response.getHeaders().getLocation().toString().contains("/promotions"));
  }

  @Test
  void download_action_redirects_to_presigned_url() {
    loginAsAdmin(restTemplate);
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
    seeder.track(TrackCode.EL, "Écosystème Logiciel");

    var response = postDiplomas(cohort, "download", "EL");

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertEquals(
        "http://localhost/diplomas.xlsx", response.getHeaders().getLocation().toString());
  }

  @Test
  void non_admin_is_forbidden() {
    seedUser("student.web@cu.te", Role.STUDENT);
    loginAsUser(restTemplate, "student.web@cu.te");
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);

    var page = restTemplate.getForEntity("/promotions", String.class);
    assertEquals(HttpStatus.FORBIDDEN, page.getStatusCode());

    var post = postDiplomas(cohort, "download", null);
    assertEquals(HttpStatus.FORBIDDEN, post.getStatusCode());
  }

  @Test
  void post_without_csrf_is_rejected() {
    loginAsAdmin(restTemplate);
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("action", "generate");

    var response =
        restTemplate.postForEntity(
            "/promotions/" + cohort.getId() + "/diplomas",
            new HttpEntity<>(formData, headers),
            String.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }
}