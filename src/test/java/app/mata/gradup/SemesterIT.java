package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.SemesterCreateRequest;
import app.mata.gradup.endpoint.rest.model.SemesterResponse;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public class SemesterIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private SemesterRepository semesterRepository;

  private JAcademicYear yearOne;
  private JAcademicYear yearTwo;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
    yearOne = seeder.academicYear("2021-2022", LocalDate.of(2021, 9, 1), LocalDate.of(2022, 7, 31));
    yearTwo = seeder.academicYear("2022-2023", LocalDate.of(2022, 9, 1), LocalDate.of(2023, 7, 31));
  }

  @Test
  void listSemesters_shouldReturnAllSemesters() {
    seeder.semester(1, yearOne, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
    seeder.semester(2, yearTwo, LocalDate.of(2022, 9, 1), LocalDate.of(2023, 1, 31));

    var response = restTemplate.getForEntity("/semesters", SemesterResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var semesters = List.of(response.getBody());
    assertEquals(2, semesters.size());
    assertEquals(
        List.of(1, 2), semesters.stream().map(SemesterResponse::getNumber).sorted().toList());
  }

  @Test
  void listSemesters_shouldFilterByAcademicYear() {
    seeder.semester(1, yearOne, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
    seeder.semester(1, yearTwo, LocalDate.of(2022, 9, 1), LocalDate.of(2023, 1, 31));

    var response =
        restTemplate.getForEntity(
            "/semesters?academicYearId=" + yearOne.getId(), SemesterResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var semesters = List.of(response.getBody());
    assertEquals(1, semesters.size());
    assertEquals(yearOne.getId(), semesters.get(0).getAcademicYear().getId());
  }

  @Test
  void createSemester_shouldReturnCreatedSemester() {
    var response =
        restTemplate.postForEntity(
            "/semesters",
            new SemesterCreateRequest()
                .number(1)
                .academicYearId(yearOne.getId())
                .startDate(LocalDate.of(2021, 9, 1))
                .endDate(LocalDate.of(2022, 1, 31)),
            SemesterResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var semester = response.getBody();
    assertNotNull(semester);
    assertNotNull(semester.getId());
    assertEquals(1, semester.getNumber());
    assertEquals(yearOne.getId(), semester.getAcademicYear().getId());
    assertEquals("2021-2022", semester.getAcademicYear().getLabel());

    var saved = semesterRepository.findById(semester.getId());
    assertTrue(saved.isPresent());
    assertEquals(1, saved.get().getNumber());
  }

  @Test
  void createSemester_shouldReturn404_whenUnknownAcademicYear() {
    var response =
        restTemplate.postForEntity(
            "/semesters",
            new SemesterCreateRequest()
                .number(1)
                .academicYearId(UUID.randomUUID())
                .startDate(LocalDate.of(2021, 9, 1))
                .endDate(LocalDate.of(2022, 1, 31)),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void createSemester_shouldReturn409_whenDuplicateNumberInAcademicYear() {
    seeder.semester(1, yearOne, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));

    var response =
        restTemplate.postForEntity(
            "/semesters",
            new SemesterCreateRequest()
                .number(1)
                .academicYearId(yearOne.getId())
                .startDate(LocalDate.of(2021, 9, 1))
                .endDate(LocalDate.of(2022, 1, 31)),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    var error = response.getBody();
    assertNotNull(error);
    assertEquals("CONFLICT", error.getCode());
  }

  @Test
  void listSemesters_shouldAllowAnyAuthenticatedUser() {
    seedUser("student@cu.te", Role.STUDENT);
    loginAs(restTemplate, "student@cu.te");

    var response = restTemplate.getForEntity("/semesters", SemesterResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void createSemester_shouldReturn403_forStudent() {
    seedUser("student@cu.te", Role.STUDENT);
    loginAs(restTemplate, "student@cu.te");

    var response =
        restTemplate.postForEntity(
            "/semesters",
            new SemesterCreateRequest()
                .number(1)
                .academicYearId(yearOne.getId())
                .startDate(LocalDate.of(2021, 9, 1))
                .endDate(LocalDate.of(2022, 1, 31)),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    var error = response.getBody();
    assertNotNull(error);
    assertEquals("FORBIDDEN", error.getCode());
  }
}
