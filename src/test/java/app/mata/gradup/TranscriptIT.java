package app.mata.gradup;

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
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.TranscriptDetailRepository;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGrade;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JStudentGroupHistory;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.repository.model.JTranscriptDetail;
import app.mata.gradup.repository.model.JUser;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.transaction.support.TransactionTemplate;

class TranscriptIT extends SecuredFacadeIT {

  private static final String BASE_URL = "/students/%s/transcripts";

  @MockBean private BucketComponent bucketComponent;
  @MockBean private EventProducer eventProducer;
  @MockBean private Mailer mailer;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @Autowired private UserRepository userRepository;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private TrackRepository trackRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private AcademicYearRepository academicYearRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private StudentGroupHistoryRepository studentGroupHistoryRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseOfferingRepository courseOfferingRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private TranscriptRepository transcriptRepository;
  @Autowired private TranscriptDetailRepository transcriptDetailRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() throws Exception {
    reset(bucketComponent, eventProducer, mailer);
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
    when(bucketComponent.presign(any(), any()))
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
        0,
        new BigDecimal("12.50")
            .compareTo(BigDecimal.valueOf(json.getOverallAverage().doubleValue())));
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
  void post_with_mismatched_ids_returns_400() {
    Fixture fixture = seed(false);
    String body =
        """
        {"type":"FULL","semesterId":"%s"}
        """
            .formatted(fixture.semesterId);

    ResponseEntity<String> response = rawPost(fixture.studentId, body);

    assertEquals(400, response.getStatusCode().value());
    Assertions.assertNotNull(response.getBody());
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
    Assertions.assertNotNull(response.getBody());
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
    return transactionTemplate.execute(
        status -> {
          JUser user =
              userRepository.save(
                  JUser.builder()
                      .lastName("Mathieu")
                      .firstName("Tafita")
                      .email("tafita@cu.te")
                      .passwordHash("hashed")
                      .role(Role.STUDENT)
                      .build());
          JCohort cohort =
              cohortRepository.save(
                  JCohort.builder()
                      .label("Mpamakilay")
                      .entryYear(2021)
                      .expectedGraduationYear(2024)
                      .build());
          JStudent student =
              studentRepository.save(JStudent.builder().user(user).cohort(cohort).build());

          JTrack track =
              trackRepository.save(
                  JTrack.builder().code(TrackCode.EL).label("Ecosysteme Logiciel").build());
          JGroup group =
              groupRepository.save(
                  JGroup.builder().reference("k1").cohort(cohort).track(track).build());
          JAcademicYear year =
              academicYearRepository.save(
                  JAcademicYear.builder()
                      .label("2025-2026")
                      .startDate(LocalDate.of(2025, 9, 1))
                      .endDate(LocalDate.of(2026, 7, 31))
                      .build());
          JSemester semester =
              semesterRepository.save(
                  JSemester.builder()
                      .number(1)
                      .academicYear(year)
                      .startDate(LocalDate.of(2025, 9, 1))
                      .endDate(LocalDate.of(2026, 1, 31))
                      .build());
          studentGroupHistoryRepository.save(
              JStudentGroupHistory.builder()
                  .student(student)
                  .group(group)
                  .startDate(LocalDate.of(2025, 9, 1))
                  .endDate(null)
                  .build());

          JCourse course =
              courseRepository.save(
                  JCourse.builder()
                      .reference("PROG1")
                      .title("Programming")
                      .credits(6)
                      .semesterNumber(1)
                      .track(track)
                      .build());
          JCourseOffering offering =
              courseOfferingRepository.save(
                  JCourseOffering.builder()
                      .course(course)
                      .group(group)
                      .semester(semester)
                      .gradingFinalized(true)
                      .build());

          if (withGrade) {
            JExam exam =
                examRepository.save(
                    JExam.builder()
                        .offering(offering)
                        .label("Final")
                        .examDate(LocalDate.of(2025, 12, 10))
                        .weightNumerator(1)
                        .weightDenominator(1)
                        .build());
            gradeRepository.save(
                JGrade.builder()
                    .student(student)
                    .exam(exam)
                    .score(new BigDecimal("12.50"))
                    .recordedAt(Instant.now())
                    .recordedBy(user.getId())
                    .build());
          }

          return new Fixture(student.getId(), semester.getId(), year.getId());
        });
  }

  private record Fixture(UUID studentId, UUID semesterId, UUID academicYearId) {}
}
