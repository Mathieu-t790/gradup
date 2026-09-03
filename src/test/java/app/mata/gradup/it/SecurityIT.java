package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.StudentCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentResponse;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.DummyUuidRepository;
import app.mata.gradup.repository.model.DummyUuid;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JStudent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;

class SecurityIT extends SecuredFacadeIT {

  @MockBean private Mailer mailer;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private DummyUuidRepository dummyUuidRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
  }

  @Test
  void anonymous_requests_are_rejected_with_401() {
    var response = restTemplate.getForEntity("/students/" + UUID.randomUUID(), Error.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UNAUTHORIZED", response.getBody().getCode());
  }

  @Test
  void wrong_password_keeps_user_unauthenticated() {
    seedUser("nobody@cu.te", Role.STUDENT);
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("email", "nobody@cu.te");
    formData.add("password", "wrong-password");
    formData.add("_csrf", csrfToken(restTemplate));
    restTemplate.postForEntity("/login", new HttpEntity<>(formData, headers), String.class);

    var response = restTemplate.getForEntity("/students/" + UUID.randomUUID(), Error.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void student_cannot_access_admin_endpoints() {
    seedUser("tafita@cu.te", Role.STUDENT);
    loginAsUser(restTemplate, "tafita@cu.te");
    var cohort = saveCohort();
    var group = saveGroup(cohort);

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("new.student@cu.te")
                .cohortId(cohort.getId())
                .initialGroupId(group.getId()),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void teacher_cannot_access_admin_endpoints() {
    seedUser("teacher@cu.te", Role.TEACHER);
    loginAsUser(restTemplate, "teacher@cu.te");

    var response = restTemplate.postForEntity("/students", new StudentCreateRequest(), Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void teacher_cannot_access_any_student_profile() {
    var student = saveStudent("tafita@cu.te");
    seedUser("teacher@cu.te", Role.TEACHER);
    loginAsUser(restTemplate, "teacher@cu.te");

    var response = restTemplate.getForEntity("/students/" + student.getId(), Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void admin_can_create_student() {
    loginAsAdmin(restTemplate);
    var cohort = saveCohort();
    var group = saveGroup(cohort);

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te")
                .cohortId(cohort.getId())
                .initialGroupId(group.getId()),
            StudentResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getId());
  }

  @Test
  void health_event_uuids_does_not_require_csrf_token() {
    var uuid = UUID.randomUUID().toString();
    dummyUuidRepository.deleteAll();
    var dummy = new DummyUuid();
    dummy.setId(uuid);
    dummyUuidRepository.save(dummy);

    var response = restTemplate.postForEntity("/health/event/uuids", List.of(uuid), String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("OK", response.getBody());
  }

  @Test
  void student_accesses_own_profile_only() {
    var own = saveStudent("tafita@cu.te");
    var other = saveStudent("other@cu.te");
    loginAsUser(restTemplate, "tafita@cu.te");

    var ownResponse = restTemplate.getForEntity("/students/" + own.getId(), StudentResponse.class);
    var otherResponse = restTemplate.getForEntity("/students/" + other.getId(), Error.class);

    assertEquals(HttpStatus.OK, ownResponse.getStatusCode());
    assertNotNull(ownResponse.getBody());
    assertEquals("tafita@cu.te", ownResponse.getBody().getEmail());
    assertEquals(HttpStatus.FORBIDDEN, otherResponse.getStatusCode());
    assertNotNull(otherResponse.getBody());
    assertEquals("FORBIDDEN", otherResponse.getBody().getCode());
  }

  private JStudent saveStudent(String email) {
    var cohort = saveCohort();
    return seeder.studentWithoutHistories(null, "Mathieu", "Tafita", email, cohort);
  }

  private JCohort saveCohort() {
    return seeder.cohort("Mpamakilay", 2021, 2024);
  }

  private JGroup saveGroup(JCohort cohort) {
    return seeder.group("K1", cohort, null);
  }
}
