package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.TrackCode;
import app.mata.gradup.endpoint.rest.model.TrackCreateRequest;
import app.mata.gradup.endpoint.rest.model.TrackResponse;
import app.mata.gradup.repository.TrackRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

public class TrackIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private TrackRepository trackRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void listTracks_shouldReturnAllTracks() {
    seeder.track("EL", "Ecosysteme Logiciel");
    seeder.track("TN", "Transformation Numerique");

    var response = restTemplate.getForEntity("/tracks", TrackResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
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
    assertEquals(TrackCode.EL.name(), saved.get().getCode().name());
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
    seeder.track("EL", "Ecosysteme Logiciel");

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
}
