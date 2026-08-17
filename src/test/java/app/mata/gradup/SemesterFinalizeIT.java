package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.SemesterCreditValidationResponse;
import app.mata.gradup.endpoint.rest.model.TrackCode;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

public class SemesterFinalizeIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  private Fixture seedSemesterWithCredits(int... credits) {
    return seeder.inTransaction(
        () -> {
          var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
          var track = seeder.track("EL", "Ecosysteme Logiciel");
          var group = seeder.group("K1", cohort, track);
          var year =
              seeder.academicYear("2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
          var semester =
              seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
          var number = 1;
          for (int credit : credits) {
            seeder.offering(seeder.course("COURSE" + number++, credit, 1, track), group, semester);
          }
          return new Fixture(semester.getId(), track.getId());
        });
  }

  private String finalizeUrl(Fixture fixture) {
    return "/semesters/" + fixture.semesterId() + "/finalize?trackId=" + fixture.trackId();
  }

  @Test
  void finalize_withThirtyCredits_returnsCreatedValidation() {
    var fixture = seedSemesterWithCredits(30);

    var response =
        restTemplate.postForEntity(
            finalizeUrl(fixture), HttpEntity.EMPTY, SemesterCreditValidationResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var body = response.getBody();
    assertNotNull(body);
    assertEquals(30, body.getTotalCredits());
    assertEquals(TrackCode.EL, body.getTrack().getCode());
    assertEquals(fixture.semesterId(), body.getSemester().getId());
    assertNotNull(body.getValidatedAt());
    assertNotNull(body.getValidatedByName());
  }

  @Test
  void finalize_twice_returnsConflict() {
    var fixture = seedSemesterWithCredits(30);

    var first =
        restTemplate.postForEntity(
            finalizeUrl(fixture), HttpEntity.EMPTY, SemesterCreditValidationResponse.class);
    assertEquals(HttpStatus.CREATED, first.getStatusCode());

    var second = restTemplate.postForEntity(finalizeUrl(fixture), HttpEntity.EMPTY, Error.class);

    assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
    assertNotNull(second.getBody());
    assertEquals("CONFLICT", second.getBody().getCode());
  }

  @Test
  void finalize_withWrongCredits_returnsUnprocessableEntityWithActualTotal() {
    var fixture = seedSemesterWithCredits(20, 20);

    var response = restTemplate.postForEntity(finalizeUrl(fixture), HttpEntity.EMPTY, Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().getMessage().contains("40"));
  }

  @Test
  void finalize_withUnknownSemester_returnsNotFound() {
    var fixture = seedSemesterWithCredits(30);

    var response =
        restTemplate.postForEntity(
            "/semesters/" + UUID.randomUUID() + "/finalize?trackId=" + fixture.trackId(),
            HttpEntity.EMPTY,
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void finalize_withUnknownTrack_returnsNotFound() {
    var fixture = seedSemesterWithCredits(30);

    var response =
        restTemplate.postForEntity(
            "/semesters/" + fixture.semesterId() + "/finalize?trackId=" + UUID.randomUUID(),
            HttpEntity.EMPTY,
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void finalize_commonCoreCourse_offeredInBothTracks_countsOnlyForGivenTrack() {
    var fixture =
        seeder.inTransaction(
            () -> {
              var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
              var el = seeder.track("EL", "Ecosysteme Logiciel");
              var tn = seeder.track("TN", "Transformation Numerique");
              var groupEl = seeder.group("K1", cohort, el);
              var groupTn = seeder.group("K2", cohort, tn);
              var year =
                  seeder.academicYear(
                      "2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
              var semester =
                  seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
              var commonCourse = seeder.course("COMMON", 30, 1, null);
              seeder.offering(commonCourse, groupEl, semester);
              seeder.offering(commonCourse, groupTn, semester);
              return new Fixture(semester.getId(), el.getId());
            });

    var response =
        restTemplate.postForEntity(
            finalizeUrl(fixture), HttpEntity.EMPTY, SemesterCreditValidationResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(30, response.getBody().getTotalCredits());
  }

  private record Fixture(UUID semesterId, UUID trackId) {}
}
