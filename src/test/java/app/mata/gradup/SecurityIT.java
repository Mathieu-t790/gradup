package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.StudentCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentResponse;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JUser;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
  @Autowired private CohortRepository cohortRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private StudentGroupHistoryRepository groupHistoryRepository;
  @Autowired private StudentTrackHistoryRepository trackHistoryRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TestDataSeeder seeder;

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
    loginAs(restTemplate, "tafita@cu.te");
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
    loginAs(restTemplate, "teacher@cu.te");

    var response = restTemplate.postForEntity("/students", new StudentCreateRequest(), Error.class);

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
  void student_accesses_own_profile_only() {
    var own = saveStudent("tafita@cu.te");
    var other = saveStudent("other@cu.te");
    loginAs(restTemplate, "tafita@cu.te");

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
    return inTransaction(
        () -> {
          var managedCohort = cohortRepository.findById(saveCohort().getId()).orElseThrow();
          var user =
              userRepository.save(
                  JUser.builder()
                      .lastName("Mathieu")
                      .firstName("Tafita")
                      .email(email)
                      .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                      .role(Role.STUDENT)
                      .isActive(true)
                      .build());
          return studentRepository.save(
              JStudent.builder().user(user).cohort(managedCohort).build());
        });
  }

  private <T> T inTransaction(java.util.function.Supplier<T> action) {
    return seeder.inTransaction(action);
  }

  private JCohort saveCohort() {
    return cohortRepository.save(
        JCohort.builder().label("Mpamakilay").entryYear(2021).expectedGraduationYear(2024).build());
  }

  private JGroup saveGroup(JCohort cohort) {
    return groupRepository.save(JGroup.builder().reference("K1").cohort(cohort).build());
  }
}
