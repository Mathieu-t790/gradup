package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.FacadeIT;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.GroupCreateRequest;
import app.mata.gradup.endpoint.rest.model.GroupResponse;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JTrack;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GroupIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private TrackRepository trackRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private StudentGroupHistoryRepository groupHistoryRepository;
  @Autowired private StudentTrackHistoryRepository trackHistoryRepository;

  @BeforeEach
  void configureRestTemplate() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void cleanDatabase() {
    groupHistoryRepository.deleteAll();
    trackHistoryRepository.deleteAll();
    studentRepository.deleteAll();
    groupRepository.deleteAll();
    cohortRepository.deleteAll();
    trackRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void listGroups_noGroups_returnsEmptyList() {
    var response = restTemplate.getForEntity("/groups", GroupResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(0, List.of(response.getBody()).size());
  }

  @Test
  void listGroups_returnsAllGroups() {
    var cohort = saveCohort();
    saveGroup(cohort, "K1");
    saveGroup(cohort, "K2");

    var response = restTemplate.getForEntity("/groups", GroupResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var groups = List.of(response.getBody());
    assertEquals(2, groups.size());
  }

  @Test
  void listGroups_filterByCohortId() {
    var firstCohort = saveCohort();
    var secondCohort = saveCohort();
    saveGroup(firstCohort, "K1");
    saveGroup(firstCohort, "K2");
    saveGroup(secondCohort, "L1");

    var response =
        restTemplate.getForEntity("/groups?cohortId=" + firstCohort.getId(), GroupResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var groups = List.of(response.getBody());
    assertEquals(2, groups.size());
    assertTrue(
        groups.stream().allMatch(group -> group.getCohort().getId().equals(firstCohort.getId())));
  }

  @Test
  void listGroups_filterByTrackId() {
    var cohort = saveCohort();
    var track = saveTrack("EL", "EL (4 years)");
    saveGroup(cohort, "K1", track);
    saveGroup(cohort, "K2");

    var response =
        restTemplate.getForEntity("/groups?trackId=" + track.getId(), GroupResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var groups = List.of(response.getBody());
    assertEquals(1, groups.size());
    assertEquals("K1", groups.get(0).getReference());
    assertEquals(track.getId(), groups.get(0).getTrack().getId());
  }

  @Test
  void createGroup_withoutTrack_returnsCreatedGroup() {
    var cohort = saveCohort();

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
    var cohort = saveCohort();
    var track = saveTrack("EL", "EL (4 years)");

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
    assertEquals(track.getId(), group.getTrack().getId());
    assertEquals("EL", group.getTrack().getCode().toString());
  }

  @Test
  void createGroup_sameReferenceInDifferentCohort_returnsCreated() {
    var firstCohort = saveCohort();
    var secondCohort = saveCohort();
    saveGroup(firstCohort, "K1");

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest().reference("K1").cohortId(secondCohort.getId()),
            GroupResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(secondCohort.getId(), response.getBody().getCohort().getId());
  }

  @Test
  void createGroup_blankReference_returnsBadRequest() {
    var cohort = saveCohort();

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest().reference("  ").cohortId(cohort.getId()),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void createGroup_missingCohort_returnsBadRequest() {
    var response =
        restTemplate.postForEntity(
            "/groups", new GroupCreateRequest().reference("K1"), Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void createGroup_unknownTrack_returnsNotFound() {
    var cohort = saveCohort();

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest()
                .reference("K1")
                .cohortId(cohort.getId())
                .trackId(UUID.randomUUID()),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void createGroup_duplicateReferenceInSameCohort_returnsConflict() {
    var cohort = saveCohort();
    saveGroup(cohort, "K1");

    var response =
        restTemplate.postForEntity(
            "/groups",
            new GroupCreateRequest().reference("K1").cohortId(cohort.getId()),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  private JCohort saveCohort() {
    return cohortRepository.save(
        JCohort.builder().label("P14").entryYear(2024).expectedGraduationYear(2027).build());
  }

  private JTrack saveTrack(String code, String label) {
    return trackRepository.save(
        JTrack.builder().code(app.mata.gradup.model.TrackCode.valueOf(code)).label(label).build());
  }

  private JGroup saveGroup(JCohort cohort, String reference) {
    return saveGroup(cohort, reference, null);
  }

  private JGroup saveGroup(JCohort cohort, String reference, JTrack track) {
    return groupRepository.save(
        JGroup.builder().reference(reference).cohort(cohort).track(track).build());
  }
}
