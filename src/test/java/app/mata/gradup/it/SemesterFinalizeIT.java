package app.mata.gradup.it;

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

  private String finalizeUrl(UUID semesterId, UUID trackId) {
    return "/semesters/" + semesterId + "/finalize?trackId=" + trackId;
  }

  private String finalizeUrl(Fixture fixture) {
    return finalizeUrl(fixture.semesterId(), fixture.trackId());
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

  @Test
  void finalize_commonCoreGroup_countsForBothTracks() {
    var fixture =
        seeder.inTransaction(
            () -> {
              var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
              var el = seeder.track("EL", "Ecosysteme Logiciel");
              var tn = seeder.track("TN", "Transformation Numerique");
              var commonGroup = seeder.group("K1", cohort, null);
              var year =
                  seeder.academicYear(
                      "2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
              var semester =
                  seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
              var commonCourse = seeder.course("COMMON", 30, 1, null);
              seeder.offering(commonCourse, commonGroup, semester);
              return new TracksFixture(semester.getId(), el.getId(), tn.getId());
            });

    var elResponse =
        restTemplate.postForEntity(
            finalizeUrl(fixture.semesterId(), fixture.elTrackId()),
            HttpEntity.EMPTY,
            SemesterCreditValidationResponse.class);
    assertEquals(HttpStatus.CREATED, elResponse.getStatusCode());
    assertNotNull(elResponse.getBody());
    assertEquals(30, elResponse.getBody().getTotalCredits());

    var tnResponse =
        restTemplate.postForEntity(
            finalizeUrl(fixture.semesterId(), fixture.tnTrackId()),
            HttpEntity.EMPTY,
            SemesterCreditValidationResponse.class);
    assertEquals(HttpStatus.CREATED, tnResponse.getStatusCode());
    assertNotNull(tnResponse.getBody());
    assertEquals(30, tnResponse.getBody().getTotalCredits());
  }

  @Test
  void finalize_commonCoursesInTrackSemester_countedForEachTrack() {
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
              var commonCourse = seeder.course("COMMON", 20, 1, null);
              var elCourse = seeder.course("ELONLY", 10, 1, el);
              var tnCourse = seeder.course("TNONLY", 10, 1, tn);
              seeder.offering(commonCourse, groupEl, semester);
              seeder.offering(commonCourse, groupTn, semester);
              seeder.offering(elCourse, groupEl, semester);
              seeder.offering(tnCourse, groupTn, semester);
              return new TracksFixture(semester.getId(), el.getId(), tn.getId());
            });

    var elResponse =
        restTemplate.postForEntity(
            finalizeUrl(fixture.semesterId(), fixture.elTrackId()),
            HttpEntity.EMPTY,
            SemesterCreditValidationResponse.class);
    assertEquals(HttpStatus.CREATED, elResponse.getStatusCode());
    assertNotNull(elResponse.getBody());
    assertEquals(30, elResponse.getBody().getTotalCredits());

    var tnResponse =
        restTemplate.postForEntity(
            finalizeUrl(fixture.semesterId(), fixture.tnTrackId()),
            HttpEntity.EMPTY,
            SemesterCreditValidationResponse.class);
    assertEquals(HttpStatus.CREATED, tnResponse.getStatusCode());
    assertNotNull(tnResponse.getBody());
    assertEquals(30, tnResponse.getBody().getTotalCredits());
  }

  @Test
  void finalize_duplicateOfferingsAcrossGroups_doesNotDoubleCount() {
    var fixture =
        seeder.inTransaction(
            () -> {
              var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
              var el = seeder.track("EL", "Ecosysteme Logiciel");
              var groupEl1 = seeder.group("K1", cohort, el);
              var groupEl2 = seeder.group("K2", cohort, el);
              var year =
                  seeder.academicYear(
                      "2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
              var semester =
                  seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
              var course = seeder.course("PROG", 30, 1, null);
              seeder.offering(course, groupEl1, semester);
              seeder.offering(course, groupEl2, semester);
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

  private record TracksFixture(UUID semesterId, UUID elTrackId, UUID tnTrackId) {}
}
