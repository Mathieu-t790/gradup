package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.TrackCode;
import app.mata.gradup.endpoint.rest.model.TrackCreateRequest;
import app.mata.gradup.endpoint.rest.model.TrackResponse;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JTrack;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public class TrackIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TrackRepository trackRepository;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private GroupRepository groupRepository;
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
  void listTracks_shouldReturnAllTracks() {
    saveTrack("EL", "Ecosysteme Logiciel");
    saveTrack("TN", "Transformation Numerique");

    var response = restTemplate.getForEntity("/tracks", TrackResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var tracks = List.of(response.getBody());
    assertEquals(2, tracks.size());
    assertEquals(
        List.of(TrackCode.EL, TrackCode.TN),
        tracks.stream().map(TrackResponse::getCode).sorted().toList());
    assertEquals(
        List.of("Ecosysteme Logiciel", "Transformation Numerique"),
        tracks.stream().map(TrackResponse::getLabel).sorted().toList());
  }

  @Test
  void createTrack_shouldReturnCreatedTrack() {
    var response =
        restTemplate.postForEntity(
            "/tracks",
            new TrackCreateRequest().code(TrackCode.EL).label("Ecosysteme Logiciel"),
            TrackResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var track = response.getBody();
    assertNotNull(track);
    assertNotNull(track.getId());
    assertEquals(TrackCode.EL, track.getCode());
    assertEquals("Ecosysteme Logiciel", track.getLabel());

    var saved = trackRepository.findById(track.getId());
    assertTrue(saved.isPresent());
    assertEquals(app.mata.gradup.model.TrackCode.EL, saved.get().getCode());
    assertEquals("Ecosysteme Logiciel", saved.get().getLabel());
  }

  @Test
  void createTrack_shouldReturn400_whenBlankLabel() {
    var response =
        restTemplate.postForEntity(
            "/tracks", new TrackCreateRequest().code(TrackCode.EL).label(""), Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void createTrack_shouldReturn409_whenDuplicateCode() {
    saveTrack("EL", "Ecosysteme Logiciel");

    var response =
        restTemplate.postForEntity(
            "/tracks",
            new TrackCreateRequest().code(TrackCode.EL).label("Duplicated"),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    var error = response.getBody();
    assertNotNull(error);
    assertEquals("CONFLICT", error.getCode());
  }

  private JTrack saveTrack(String code, String label) {
    return trackRepository.save(
        JTrack.builder().code(app.mata.gradup.model.TrackCode.valueOf(code)).label(label).build());
  }
}
