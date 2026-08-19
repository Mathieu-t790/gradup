package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.AcademicYearCreateRequest;
import app.mata.gradup.endpoint.rest.model.AcademicYearResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.AcademicYearRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public class AcademicYearIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private AcademicYearRepository academicYearRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void listAcademicYears_shouldReturnAllAcademicYears() {
    seeder.academicYear("2021-2022", LocalDate.of(2021, 9, 1), LocalDate.of(2022, 7, 31));
    seeder.academicYear("2022-2023", LocalDate.of(2022, 9, 1), LocalDate.of(2023, 7, 31));

    var response = restTemplate.getForEntity("/academic-years", AcademicYearResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var years = List.of(response.getBody());
    assertEquals(2, years.size());
    assertEquals(
        List.of("2021-2022", "2022-2023"),
        years.stream().map(AcademicYearResponse::getLabel).sorted().toList());
  }

  @Test
  void createAcademicYear_shouldReturnCreatedAcademicYear() {
    var response =
        restTemplate.postForEntity(
            "/academic-years",
            new AcademicYearCreateRequest()
                .label("2021-2022")
                .startDate(LocalDate.of(2021, 9, 1))
                .endDate(LocalDate.of(2022, 7, 31)),
            AcademicYearResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var year = response.getBody();
    assertNotNull(year);
    assertNotNull(year.getId());
    assertEquals("2021-2022", year.getLabel());
    assertEquals(LocalDate.of(2021, 9, 1), year.getStartDate());
    assertEquals(LocalDate.of(2022, 7, 31), year.getEndDate());

    var saved = academicYearRepository.findById(year.getId());
    assertTrue(saved.isPresent());
    assertEquals("2021-2022", saved.get().getLabel());
  }

  @Test
  void createAcademicYear_shouldReturn400_whenBlankLabel() {
    var response =
        restTemplate.postForEntity(
            "/academic-years",
            new AcademicYearCreateRequest()
                .label("")
                .startDate(LocalDate.of(2021, 9, 1))
                .endDate(LocalDate.of(2022, 7, 31)),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createAcademicYear_shouldReturn400_whenEndDateNotAfterStartDate() {
    var response =
        restTemplate.postForEntity(
            "/academic-years",
            new AcademicYearCreateRequest()
                .label("2021-2022")
                .startDate(LocalDate.of(2021, 9, 1))
                .endDate(LocalDate.of(2021, 9, 1)),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createAcademicYear_shouldReturn409_whenDuplicateLabel() {
    seeder.academicYear("2021-2022", LocalDate.of(2021, 9, 1), LocalDate.of(2022, 7, 31));

    var response =
        restTemplate.postForEntity(
            "/academic-years",
            new AcademicYearCreateRequest()
                .label("2021-2022")
                .startDate(LocalDate.of(2021, 9, 1))
                .endDate(LocalDate.of(2022, 7, 31)),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    var error = response.getBody();
    assertNotNull(error);
    assertEquals("CONFLICT", error.getCode());
  }

  @Test
  void listAcademicYears_shouldAllowAnyAuthenticatedUser() {
    seedUser("student@cu.te", Role.STUDENT);
    loginAsUser(restTemplate, "student@cu.te");

    var response = restTemplate.getForEntity("/academic-years", AcademicYearResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void createAcademicYear_shouldReturn403_forStudent() {
    seedUser("student@cu.te", Role.STUDENT);
    loginAsUser(restTemplate, "student@cu.te");

    var response =
        restTemplate.postForEntity(
            "/academic-years",
            new AcademicYearCreateRequest()
                .label("2021-2022")
                .startDate(LocalDate.of(2021, 9, 1))
                .endDate(LocalDate.of(2022, 7, 31)),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    var error = response.getBody();
    assertNotNull(error);
    assertEquals("FORBIDDEN", error.getCode());
  }
}
