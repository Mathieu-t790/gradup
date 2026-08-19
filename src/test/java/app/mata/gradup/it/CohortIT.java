package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.CohortCreateRequest;
import app.mata.gradup.endpoint.rest.model.CohortResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.repository.CohortRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public class CohortIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private CohortRepository cohortRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
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
    seeder.cohort("Promo 2024", 2024, 2027);
    seeder.cohort("Promo 2025", 2025, 2028);

    var response = restTemplate.getForEntity("/cohorts", CohortResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var cohorts = List.of(response.getBody());
    assertEquals(2, cohorts.size());
    assertEquals(
        List.of("Promo 2024", "Promo 2025"),
        cohorts.stream().map(CohortResponse::getLabel).sorted().toList());
  }

  @Test
  void getCohort_shouldReturnCohort_whenExists() {
    var saved = seeder.cohort("Promo 2026", 2026, 2029);

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
    assertTrue(error.getMessage().contains("Cohort not found: " + randomId));
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
}
