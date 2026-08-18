package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.event.EventProducer;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.CohortRepository;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
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

class WebUiIT extends SecuredFacadeIT {

  private static final Pattern CSRF_PATTERN =
      Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

  @MockBean private BucketComponent bucketComponent;
  @MockBean private EventProducer eventProducer;
  @MockBean private Mailer mailer;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private CohortRepository cohortRepository;

  @BeforeEach
  void setUp() throws Exception {
    reset(bucketComponent, eventProducer, mailer);
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    when(bucketComponent.presign(any(), any()))
        .thenReturn(URI.create("http://localhost/download.pdf").toURL());
  }

  @Test
  void anonymous_web_pages_redirect_to_login() {
    var response = restTemplate.getForEntity("/web/cohorts", String.class);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertNotNull(response.getHeaders().getLocation());
  }

  @Test
  void authenticated_user_can_view_web_pages() {
    loginAsAdmin(restTemplate);
    seeder.cohort("Mpamakilay", 2021, 2024);

    var response = restTemplate.getForEntity("/web/cohorts", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("Mpamakilay"));
    assertTrue(response.getBody().contains("Télécharger les diplômés"));
  }

  @Test
  void student_can_view_but_not_post_or_export_on_web() {
    seedUser("student.web@cu.te", Role.STUDENT);
    loginAsUser(restTemplate, "student.web@cu.te");
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);

    var page = restTemplate.getForEntity("/web/cohorts", String.class);
    assertEquals(HttpStatus.OK, page.getStatusCode());

    var export =
        restTemplate.getForEntity(
            "/web/cohorts/" + cohort.getId() + "/diplomas/export", String.class);
    assertEquals(HttpStatus.FORBIDDEN, export.getStatusCode());

    var post = postForm("/web/cohorts", cohortForm("PROMO 2025", 2022, 2025));
    assertEquals(HttpStatus.FORBIDDEN, post.getStatusCode());
  }

  @Test
  void post_on_web_without_csrf_is_rejected() {
    loginAsAdmin(restTemplate);

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    var response =
        restTemplate.postForEntity(
            "/web/cohorts",
            new HttpEntity<>(cohortForm("PROMO 2025", 2022, 2025), headers),
            String.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void admin_creates_cohort_through_web_form() {
    loginAsAdmin(restTemplate);

    var response = postForm("/web/cohorts", cohortForm("PROMO 2025", 2022, 2025));

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertNotNull(response.getHeaders().getLocation());
    assertEquals("/web/cohorts", response.getHeaders().getLocation().getPath());
    assertTrue(
        cohortRepository.findAll().stream()
            .anyMatch(cohort -> cohort.getLabel().equals("PROMO 2025")));
  }

  @Test
  void export_diplomas_redirects_to_presigned_url() {
    loginAsAdmin(restTemplate);
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);

    var response =
        restTemplate.getForEntity(
            "/web/cohorts/" + cohort.getId() + "/diplomas/export", String.class);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertEquals("http://localhost/download.pdf", response.getHeaders().getLocation().toString());
  }

  @Test
  void transcript_generation_redirects_to_presigned_url() {
    loginAsAdmin(restTemplate);
    Fixture fixture = seedTranscriptFixture();

    var form = new LinkedMultiValueMap<String, String>();
    form.add("semesterId", fixture.semesterId().toString());
    var response = postForm("/web/students/" + fixture.studentId() + "/transcripts", form);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertEquals("http://localhost/download.pdf", response.getHeaders().getLocation().toString());
  }

  private ResponseEntity<String> postForm(String url, LinkedMultiValueMap<String, String> form) {
    form.add("_csrf", csrfTokenFromWebPage());
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    return restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
  }

  private String csrfTokenFromWebPage() {
    var page = restTemplate.getForEntity("/web/cohorts", String.class);
    assertNotNull(page.getBody());
    var matcher = CSRF_PATTERN.matcher(page.getBody());
    return matcher.find() ? matcher.group(1) : null;
  }

  private LinkedMultiValueMap<String, String> cohortForm(
      String label, int entryYear, int graduationYear) {
    var form = new LinkedMultiValueMap<String, String>();
    form.add("label", label);
    form.add("entryYear", String.valueOf(entryYear));
    form.add("expectedGraduationYear", String.valueOf(graduationYear));
    return form;
  }

  private Fixture seedTranscriptFixture() {
    return seeder.inTransaction(
        () -> {
          var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
          var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
          var group = seeder.group("k1", cohort, track);
          var year =
              seeder.academicYear("2025-2026", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
          var semester =
              seeder.semester(1, year, LocalDate.of(2025, 9, 1), LocalDate.of(2026, 1, 31));
          var student =
              seeder.studentWithoutHistories(
                  "STD21001", "Mathieu", "Tafita", "tafita@cu.te", cohort);
          seeder.addGroupHistory(student, group, LocalDate.of(2025, 9, 1), null);
          var course = seeder.course("PROG1", "Algorithmique", 6, 1, track);
          var offering = seeder.offering(course, group, semester);
          var exam = seeder.exam(offering);
          seeder.grade(student, exam, "12.50");
          return new Fixture(student.getId(), semester.getId());
        });
  }

  private record Fixture(UUID studentId, UUID semesterId) {}
}
