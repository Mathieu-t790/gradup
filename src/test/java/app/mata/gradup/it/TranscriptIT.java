package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.event.EventProducer;
import app.mata.gradup.endpoint.event.model.TranscriptGenerated;
import app.mata.gradup.endpoint.rest.model.TranscriptResponse;
import app.mata.gradup.endpoint.rest.model.TranscriptType;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.TranscriptDetailRepository;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.model.JTranscriptDetail;
import app.mata.gradup.service.utils.DownloadPresigner;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class TranscriptIT extends SecuredFacadeIT {

  private static final String BASE_URL = "/students/%s/transcripts";

  @MockBean private BucketComponent bucketComponent;
  @MockBean private DownloadPresigner downloadPresigner;
  @MockBean private EventProducer eventProducer;
  @MockBean private Mailer mailer;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @Autowired private TranscriptRepository transcriptRepository;
  @Autowired private TranscriptDetailRepository transcriptDetailRepository;

  @BeforeEach
  void setUp() throws Exception {
    reset(bucketComponent, downloadPresigner, eventProducer, mailer);
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
    when(downloadPresigner.presign(any(), any(), any()))
        .thenReturn(URI.create("http://localhost/download.pdf").toURL());
  }

  @Test
  void post_provisional_generates_upload_and_dispatches_event() {
    Fixture fixture = seed(true);

    TranscriptResponse json =
        post(
            fixture.studentId,
            """
            {"type":"PROVISIONAL","semesterId":"%s"}
            """
                .formatted(fixture.semesterId));

    UUID transcriptId = json.getId();
    assertEquals(TranscriptType.PROVISIONAL, json.getType());
    assertTrue(json.getDownloadUrl().startsWith("http://localhost"));
    assertNull(json.getOverallAverage());
    assertNull(json.getCreditsEarned());

    String storageKey = uploadedKey(fixture.studentId, transcriptId);
    assertEquals("transcripts/" + fixture.studentId + "/" + transcriptId + ".pdf", storageKey);
    assertEquals(
        storageKey, transcriptRepository.findById(transcriptId).orElseThrow().getStorageKey());

    List<JTranscriptDetail> details =
        transcriptDetailRepository.findAll().stream()
            .filter(detail -> detail.getTranscript().getId().equals(transcriptId))
            .toList();
    assertEquals(1, details.size());
    assertTrue(details.getFirst().isCreditsEarned());
    assertEquals(new BigDecimal("12.50"), details.getFirst().getCourseScore());

    Collection<TranscriptGenerated> events = dispatchedEvents();
    assertEquals(1, events.size());
    assertEquals(transcriptId, events.iterator().next().getTranscriptId());
  }

  @Test
  void post_full_computes_overall_average_and_credits() {
    Fixture fixture = seed(true);

    TranscriptResponse json =
        post(
            fixture.studentId,
            """
            {"type":"FULL","academicYearId":"%s"}
            """
                .formatted(fixture.academicYearId));

    assertEquals(TranscriptType.FULL, json.getType());
    assertEquals(
        0, new BigDecimal("12.50").compareTo(BigDecimal.valueOf(json.getOverallAverage())));
    assertEquals(6, json.getCreditsEarned());
  }

  @Test
  void get_lists_generated_transcripts() {
    Fixture fixture = seed(false);
    TranscriptResponse created =
        post(
            fixture.studentId,
            """
            {"type":"PROVISIONAL","semesterId":"%s"}
            """
                .formatted(fixture.semesterId));

    ResponseEntity<TranscriptResponse[]> response =
        restTemplate.getForEntity(
            BASE_URL.formatted(fixture.studentId), TranscriptResponse[].class);
    TranscriptResponse[] list = response.getBody();

    assertEquals(200, response.getStatusCode().value());
    assertTrue(list != null && list.length == 1);
    assertEquals(created.getId(), list[0].getId());
    assertFalse(list[0].getDownloadUrl().isBlank());
  }

  @Test
  void get_lists_generated_transcripts_filtered_by_type() {
    Fixture fixture = seed(true);
    post(
        fixture.studentId,
        """
        {"type":"PROVISIONAL","semesterId":"%s"}
        """
            .formatted(fixture.semesterId));
    TranscriptResponse full =
        post(
            fixture.studentId,
            """
            {"type":"FULL","academicYearId":"%s"}
            """
                .formatted(fixture.academicYearId));

    TranscriptResponse[] onlyFull = list(fixture.studentId, "?type=FULL");
    TranscriptResponse[] onlyProvisional = list(fixture.studentId, "?type=PROVISIONAL");
    TranscriptResponse[] onlyDiploma = list(fixture.studentId, "?type=DIPLOMA");

    assertEquals(1, onlyFull.length);
    assertEquals(full.getId(), onlyFull[0].getId());
    assertEquals(1, onlyProvisional.length);
    assertEquals(0, onlyDiploma.length);
  }

  @Test
  void get_lists_generated_transcripts_filtered_by_semester_and_academic_year() {
    Fixture fixture = seed(true);
    TranscriptResponse provisional =
        post(
            fixture.studentId,
            """
            {"type":"PROVISIONAL","semesterId":"%s"}
            """
                .formatted(fixture.semesterId));
    TranscriptResponse full =
        post(
            fixture.studentId,
            """
            {"type":"FULL","academicYearId":"%s"}
            """
                .formatted(fixture.academicYearId));

    TranscriptResponse[] bySemester = list(fixture.studentId, "?semesterId=" + fixture.semesterId);
    TranscriptResponse[] byYear =
        list(fixture.studentId, "?academicYearId=" + fixture.academicYearId);
    TranscriptResponse[] byUnknownSemester =
        list(fixture.studentId, "?semesterId=" + UUID.randomUUID());

    assertEquals(1, bySemester.length);
    assertEquals(provisional.getId(), bySemester[0].getId());
    assertEquals(1, byYear.length);
    assertEquals(full.getId(), byYear[0].getId());
    assertEquals(0, byUnknownSemester.length);
  }

  @Test
  void post_send_dispatches_email_event_for_an_existing_transcript() throws Exception {
    Fixture fixture = seed(false);
    TranscriptResponse created =
        post(
            fixture.studentId,
            """
            {"type":"PROVISIONAL","semesterId":"%s"}
            """
                .formatted(fixture.semesterId));

    reset(eventProducer, bucketComponent, downloadPresigner, mailer);
    when(downloadPresigner.presign(any(), any(), any()))
        .thenReturn(URI.create("http://localhost/download.pdf").toURL());

    ResponseEntity<TranscriptResponse> response =
        restTemplate.postForEntity(
            BASE_URL.formatted(fixture.studentId) + "/" + created.getId() + "/send",
            null,
            TranscriptResponse.class);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(created.getId(), response.getBody().getId());
    Collection<TranscriptGenerated> events = dispatchedEvents();
    assertEquals(1, events.size());
    assertEquals(created.getId(), events.iterator().next().getTranscriptId());
  }

  @Test
  void post_send_unknown_transcript_returns_404() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            BASE_URL.formatted(UUID.randomUUID()) + "/" + UUID.randomUUID() + "/send",
            null,
            String.class);

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void post_with_mismatched_ids_returns_400() {
    Fixture fixture = seed(false);
    String body =
        """
        {"type":"FULL","semesterId":"%s"}
        """
            .formatted(fixture.semesterId);

    ResponseEntity<String> response = rawPost(fixture.studentId, body);

    assertEquals(400, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("BAD_REQUEST"));
  }

  @Test
  void post_unknown_student_returns_404() {
    String body =
        """
        {"type":"PROVISIONAL","semesterId":"%s"}
        """
            .formatted(UUID.randomUUID());

    ResponseEntity<String> response = rawPost(UUID.randomUUID(), body);

    assertEquals(404, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("NOT_FOUND"));
  }

  private TranscriptResponse post(UUID studentId, String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<TranscriptResponse> response =
        restTemplate.postForEntity(
            BASE_URL.formatted(studentId),
            new HttpEntity<>(body, headers),
            TranscriptResponse.class);
    assertEquals(200, response.getStatusCode().value());
    return response.getBody();
  }

  private TranscriptResponse[] list(UUID studentId, String query) {
    ResponseEntity<TranscriptResponse[]> response =
        restTemplate.getForEntity(
            BASE_URL.formatted(studentId) + query, TranscriptResponse[].class);
    assertEquals(200, response.getStatusCode().value());
    return response.getBody();
  }

  private ResponseEntity<String> rawPost(UUID studentId, String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.postForEntity(
        BASE_URL.formatted(studentId), new HttpEntity<>(body, headers), String.class);
  }

  private String uploadedKey(UUID studentId, UUID transcriptId) {
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponent).upload(any(), keyCaptor.capture());
    assertEquals("transcripts/" + studentId + "/" + transcriptId + ".pdf", keyCaptor.getValue());
    return keyCaptor.getValue();
  }

  private Collection<TranscriptGenerated> dispatchedEvents() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<TranscriptGenerated>> eventsCaptor =
        ArgumentCaptor.forClass(Collection.class);
    verify(eventProducer).accept(eventsCaptor.capture());
    return eventsCaptor.getValue();
  }

  private Fixture seed(boolean withGrade) {
    return seeder.inTransaction(
        () -> {
          var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
          var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
          var group = seeder.group("k1", cohort, null);
          var year =
              seeder.academicYear("2025-2026", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
          var semester =
              seeder.semester(1, year, LocalDate.of(2025, 9, 1), LocalDate.of(2026, 1, 31));
          var student =
              seeder.studentWithoutHistories(
                  "STD21001", "Mathieu", "Tafita", "tafita@cu.te", cohort);
          seeder.addGroupHistory(student, group, LocalDate.of(2025, 9, 1), null);
          var course = seeder.course("PROG1", "Algorithmique", 6, 1, track);
          var offering = seeder.offering(course, group, semester);
          if (withGrade) {
            var exam = seeder.exam(offering);
            seeder.grade(student, exam, "12.50");
          }
          return new Fixture(student.getId(), semester.getId(), year.getId());
        });
  }

  private record Fixture(UUID studentId, UUID semesterId, UUID academicYearId) {}
}
