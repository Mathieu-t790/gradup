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
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTrack;
import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;
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

  private ResponseEntity<String> postForm(String url, String... keyValues) {
    var csrf = csrfFrom(restTemplate.getForEntity("/promotions", String.class));
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("_csrf", csrf);
    for (int i = 0; i + 1 < keyValues.length; i += 2) {
      formData.add(keyValues[i], keyValues[i + 1]);
    }
    return restTemplate.postForEntity(url, new HttpEntity<>(formData, headers), String.class);
  }

  private ResponseEntity<String> postDiplomas(JCohort cohort, String action, String track) {
    return track == null
        ? postForm("/promotions/" + cohort.getId() + "/diplomas", "action", action)
        : postForm("/promotions/" + cohort.getId() + "/diplomas", "action", action, "track", track);
  }

  private JCohort seedFinishedCohortWithGrades() {
    return seeder.inTransaction(
        () -> {
          JCohort cohort = seeder.cohort("Mpamakilay", 2021, 2024);
          JTrack el = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
          JGroup group = seeder.group("G1", cohort, el);
          JAcademicYear year =
              seeder.academicYear("2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
          JSemester s1 =
              seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
          JCourse prog1 = seeder.course("PROG1", 60, 1, null);
          JExam exam = seeder.exam(seeder.offering(prog1, group, s1));
          JStudent graduated =
              seeder.student("STD21001", "Rakoto", "Hery", "hery@cu.te", cohort, el, group);
          JStudent notGraduated =
              seeder.student("STD21002", "Rabe", "Mialy", "mialy@cu.te", cohort, el, group);
          seeder.grade(graduated, exam, "12.00");
          seeder.grade(notGraduated, exam, "8.00");
          return cohort;
        });
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
    assertTrue(response.getBody().contains("Créer une promotion"));
    assertTrue(response.getBody().contains("Voir"));
    assertTrue(response.getBody().contains("Modifier"));
    assertTrue(response.getBody().contains("EL"));
    assertTrue(response.getBody().contains("TN"));
    assertTrue(response.getBody().contains("Promotions"));
    assertTrue(response.getBody().contains("Diplômés"));
  }

  @Test
  void generate_action_redirects_to_promotion_detail() {
    loginAsAdmin(restTemplate);
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);

    var response = postDiplomas(cohort, "generate", null);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertTrue(
        response.getHeaders().getLocation().toString().contains("/promotions/" + cohort.getId()));
  }

  @Test
  void download_action_redirects_to_presigned_url() {
    loginAsAdmin(restTemplate);
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
    seeder.track(TrackCode.EL, "Écosystème Logiciel");

    var response = postDiplomas(cohort, "download", "EL");

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertEquals("http://localhost/diplomas.xlsx", response.getHeaders().getLocation().toString());
  }

  @Test
  void admin_sees_detail_page_with_students_and_graduates() {
    loginAsAdmin(restTemplate);
    var cohort = seedFinishedCohortWithGrades();

    var generate = postDiplomas(cohort, "generate", null);
    assertEquals(HttpStatus.FOUND, generate.getStatusCode());

    var response = restTemplate.getForEntity("/promotions/" + cohort.getId(), String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("Mpamakilay"));
    assertTrue(response.getBody().contains("Rakoto"));
    assertTrue(response.getBody().contains("Rabe"));
    assertTrue(response.getBody().contains("Diplômé"));
    assertTrue(response.getBody().contains("Non diplômé"));
    assertTrue(response.getBody().contains("12,00"));
    assertTrue(response.getBody().contains("Générer les diplômes"));
    assertTrue(response.getBody().contains("Télécharger les diplômés"));
    assertTrue(response.getBody().contains("Retour aux promotions"));
  }

  @Test
  void unfinished_promotion_shows_banner_and_blocks_generation() {
    loginAsAdmin(restTemplate);
    var cohort = seeder.cohort("Fahazavana", 2023, 2026);
    var el = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var group = seeder.group("G1", cohort, el);
    seeder.student("STD23001", "Rasoa", "Lala", "lala@cu.te", cohort, el, group);

    var page = restTemplate.getForEntity("/promotions/" + cohort.getId(), String.class);
    System.out.println("BODY: " + page.getBody());

    assertEquals(HttpStatus.OK, page.getStatusCode());
    assertNotNull(page.getBody());
    assertTrue(page.getBody().contains("Promotion en cours"));
    assertTrue(page.getBody().contains("2027"));
    assertTrue(page.getBody().contains("Fahazavana"));
    assertTrue(page.getBody().contains("Rasoa"));
    assertTrue(page.getBody().contains("Non diplômé"));

    var generate = postDiplomas(cohort, "generate", null);

    assertEquals(HttpStatus.FOUND, generate.getStatusCode());
    assertTrue(
        Objects.requireNonNull(generate.getHeaders().getLocation())
            .toString()
            .contains("error=not.finished"));
  }

  @Test
  void detail_page_is_forbidden_for_non_admin() {
    seedUser("student.web@cu.te", Role.STUDENT);
    loginAsUser(restTemplate, "student.web@cu.te");
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);

    var response = restTemplate.getForEntity("/promotions/" + cohort.getId(), String.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void create_promotion_redirects_to_detail_and_lists() {
    loginAsAdmin(restTemplate);

    var response =
        postForm(
            "/promotions",
            "label",
            "Promo 2026",
            "entryYear",
            "2026",
            "expectedGraduationYear",
            "2029");

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    var location = response.getHeaders().getLocation().toString();
    assertTrue(location.contains("/promotions/"), "unexpected location: " + location);

    var list = restTemplate.getForEntity("/promotions", String.class);
    assertEquals(HttpStatus.OK, list.getStatusCode());
    assertNotNull(list.getBody());
    assertTrue(list.getBody().contains("Promo 2026"));
  }

  @Test
  void create_promotion_rejects_blank_label() {
    loginAsAdmin(restTemplate);

    var response =
        postForm(
            "/promotions", "label", "  ", "entryYear", "2026", "expectedGraduationYear", "2029");

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertTrue(response.getHeaders().getLocation().toString().contains("error=label.required"));
  }

  @Test
  void edit_promotion_updates_label() {
    loginAsAdmin(restTemplate);
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);

    var response = postForm("/promotions/" + cohort.getId() + "/edit", "label", "Mpamakilay 2.0");

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertTrue(response.getHeaders().getLocation().toString().contains("/promotions"));

    var list = restTemplate.getForEntity("/promotions", String.class);
    assertEquals(HttpStatus.OK, list.getStatusCode());
    assertNotNull(list.getBody());
    assertTrue(list.getBody().contains("Mpamakilay 2.0"));
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
