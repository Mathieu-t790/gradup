package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.endpoint.rest.model.CohortCreateRequest;
import app.mata.gradup.endpoint.rest.model.CohortResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JCohort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public class CohortIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private TrackRepository trackRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private StudentGroupHistoryRepository groupHistoryRepository;
  @Autowired private StudentTrackHistoryRepository trackHistoryRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @AfterEach
  void tearDown() {
    cleanDatabase();
  }

  private void cleanDatabase() {
    groupHistoryRepository.deleteAll();
    trackHistoryRepository.deleteAll();
    studentRepository.deleteAll();
    groupRepository.deleteAll();
    cohortRepository.deleteAll();
    trackRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createCohort_shouldReturnCreatedCohort() {
    var response =
        restTemplate.postForEntity(
            "/cohorts",
            new CohortCreateRequest()
                .label("Promo 2026")
                .entryYear(2026)
                .expectedGraduationYear(2029),
            CohortResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var cohort = response.getBody();
    assertNotNull(cohort);
    assertNotNull(cohort.getId());
    assertEquals("Promo 2026", cohort.getLabel());
    assertEquals(2026, cohort.getEntryYear());
    assertEquals(2029, cohort.getExpectedGraduationYear());

    var saved = cohortRepository.findById(cohort.getId());
    assertTrue(saved.isPresent());
    assertEquals("Promo 2026", saved.get().getLabel());
  }

  @Test
  void listCohorts_shouldReturnAllCohorts() {
    saveCohort("Promo 2024", 2024, 2027);
    saveCohort("Promo 2025", 2025, 2028);

    var response = restTemplate.getForEntity("/cohorts", CohortResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var cohorts = List.of(response.getBody());
    assertEquals(2, cohorts.size());
    assertEquals(
        List.of("Promo 2024", "Promo 2025"),
        cohorts.stream().map(CohortResponse::getLabel).sorted().toList());
  }

  @Test
  void getCohort_shouldReturnCohort_whenExists() {
    var saved = saveCohort("Promo 2026", 2026, 2029);

    var response = restTemplate.getForEntity("/cohorts/" + saved.getId(), CohortResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var cohort = response.getBody();
    assertNotNull(cohort);
    assertEquals(saved.getId(), cohort.getId());
    assertEquals("Promo 2026", cohort.getLabel());
    assertEquals(2026, cohort.getEntryYear());
    assertEquals(2029, cohort.getExpectedGraduationYear());
  }

  @Test
  void getCohort_shouldReturn404_whenNotFound() {
    var randomId = UUID.randomUUID();

    var response = restTemplate.getForEntity("/cohorts/" + randomId, Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    var error = response.getBody();
    assertNotNull(error);
    assertEquals("NOT_FOUND", error.getCode());
    assertTrue(error.getMessage().contains("Cohort not found with id: " + randomId));
  }

  @Test
  void createCohort_shouldReturn400_whenBlankLabel() {
    var response =
        restTemplate.postForEntity(
            "/cohorts",
            new CohortCreateRequest().label("").entryYear(2026).expectedGraduationYear(2029),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  private JCohort saveCohort(String label, int entryYear, int expectedGraduationYear) {
    return cohortRepository.save(
        JCohort.builder()
            .label(label)
            .entryYear(entryYear)
            .expectedGraduationYear(expectedGraduationYear)
            .build());
  }
}
