package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.GroupCreateRequest;
import app.mata.gradup.endpoint.rest.model.GroupResponse;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.GroupRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public class GroupIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private GroupRepository groupRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void listGroups_noGroups_returnsEmptyList() {
    var response = restTemplate.getForEntity("/groups", GroupResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, List.of(response.getBody()).size());
  }

  @Test
  void listGroups_returnsAllGroups() {
    var cohort = seeder.cohort("P14", 2024, 2027);
    seeder.group("K1", cohort, null);
    seeder.group("K2", cohort, null);

    var response = restTemplate.getForEntity("/groups", GroupResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var groups = List.of(response.getBody());
    assertEquals(2, groups.size());
  }

  @Test
  void listGroups_filterByCohortId() {
    var firstCohort = seeder.cohort("P14", 2024, 2027);
    var secondCohort = seeder.cohort("P15", 2025, 2028);
    seeder.group("K1", firstCohort, null);
    seeder.group("K2", firstCohort, null);
    seeder.group("L1", secondCohort, null);

    var response =
        restTemplate.getForEntity("/groups?cohortId=" + firstCohort.getId(), GroupResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var groups = List.of(response.getBody());
    assertEquals(2, groups.size());
    assertTrue(
        groups.stream().allMatch(group -> group.getCohort().getId().equals(firstCohort.getId())));
  }

  @Test
  void listGroups_filterByTrackId() {
    var cohort = seeder.cohort("P14", 2024, 2027);
    var track = seeder.track(TrackCode.EL, "EL (4 years)");
    seeder.group("K1", cohort, track);
    seeder.group("K2", cohort, null);

    var response =
        restTemplate.getForEntity("/groups?trackId=" + track.getId(), GroupResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var groups = List.of(response.getBody());
    assertEquals(1, groups.size());
    assertEquals("K1", groups.getFirst().getReference());
    assertEquals(track.getId(), Objects.requireNonNull(groups.getFirst().getTrack()).getId());
  }

  @Test
  void createGroup_withoutTrack_returnsCreatedGroup() {
    var cohort = seeder.cohort("P14", 2024, 2027);

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest().reference("K1").cohortId(cohort.getId()),
            GroupResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var group = response.getBody();
    assertNotNull(group);
    assertNotNull(group.getId());
    assertEquals("K1", group.getReference());
    assertEquals(cohort.getId(), group.getCohort().getId());
    assertEquals(cohort.getLabel(), group.getCohort().getLabel());
    assertNull(group.getTrack());
    assertTrue(groupRepository.existsById(group.getId()));
  }

  @Test
  void createGroup_withTrack_returnsCreatedGroup() {
    var cohort = seeder.cohort("P14", 2024, 2027);
    var track = seeder.track(TrackCode.EL, "EL (4 years)");

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest()
                .reference("K1")
                .cohortId(cohort.getId())
                .trackId(track.getId()),
            GroupResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var group = response.getBody();
    assertNotNull(group);
    assertEquals(track.getId(), Objects.requireNonNull(group.getTrack()).getId());
    assertEquals("EL", group.getTrack().getCode().toString());
  }

  @Test
  void createGroup_sameReferenceInDifferentCohort_returnsCreated() {
    var firstCohort = seeder.cohort("P14", 2024, 2027);
    var secondCohort = seeder.cohort("P15", 2025, 2028);
    seeder.group("K1", firstCohort, null);

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest().reference("K1").cohortId(secondCohort.getId()),
            GroupResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(secondCohort.getId(), response.getBody().getCohort().getId());
  }

  @Test
  void createGroup_blankReference_returnsBadRequest() {
    var cohort = seeder.cohort("P14", 2024, 2027);

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest().reference("  ").cohortId(cohort.getId()),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void createGroup_missingCohort_returnsBadRequest() {
    var response =
        restTemplate.postForEntity(
            "/groups", new GroupCreateRequest().reference("K1"), Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void createGroup_unknownCohort_returnsNotFound() {
    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest().reference("K1").cohortId(UUID.randomUUID()),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void createGroup_unknownTrack_returnsNotFound() {
    var cohort = seeder.cohort("P14", 2024, 2027);

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest()
                .reference("K1")
                .cohortId(cohort.getId())
                .trackId(UUID.randomUUID()),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void createGroup_duplicateReferenceInSameCohort_returnsConflict() {
    var cohort = seeder.cohort("P14", 2024, 2027);
    seeder.group("K1", cohort, null);

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest().reference("K1").cohortId(cohort.getId()),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }
}
