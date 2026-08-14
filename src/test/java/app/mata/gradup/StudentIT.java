package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import app.mata.gradup.conf.FacadeIT;
import app.mata.gradup.conf.TestSecurityConf;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.StudentCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentResponse;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentUpdateRequest;
import app.mata.gradup.endpoint.rest.model.TranscriptResponse;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TranscriptType;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JStudentGroupHistory;
import app.mata.gradup.repository.model.JStudentTrackHistory;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.repository.model.JTranscript;
import app.mata.gradup.repository.model.JUser;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConf.class)
public class StudentIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private TrackRepository trackRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private StudentGroupHistoryRepository groupHistoryRepository;
  @Autowired private StudentTrackHistoryRepository trackHistoryRepository;
  @Autowired private TranscriptRepository transcriptRepository;
  @Autowired private PlatformTransactionManager transactionManager;
  @MockBean private BucketComponent bucketComponent;

  @BeforeEach
  void cleanDatabase() {
    transcriptRepository.deleteAll();
    groupHistoryRepository.deleteAll();
    trackHistoryRepository.deleteAll();
    studentRepository.deleteAll();
    groupRepository.deleteAll();
    cohortRepository.deleteAll();
    trackRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createStudent_returnsCreatedStudentWithServerGeneratedFields() {
    var cohort = saveCohort();
    var group = saveGroup(cohort, "K1");

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Rakoto")
                .firstName("Jean")
                .email("jean.rakoto@hei.school")
                .cohortId(cohort.getId())
                .initialGroupId(group.getId()),
            StudentResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var student = response.getBody();
    assertNotNull(student);
    assertNotNull(student.getId());
    assertEquals("Rakoto", student.getLastName());
    assertEquals("Jean", student.getFirstName());
    assertEquals("jean.rakoto@hei.school", student.getEmail());
    assertNotNull(student.getReference());
    assertTrue(student.getReference().startsWith("STD"));
    assertEquals(cohort.getId(), student.getCohort().getId());
    assertEquals(group.getId(), student.getCurrentGroup().getId());
    assertNull(student.getCurrentTrack());
    assertNotNull(student.getEnrollmentDate());
    assertTrue(student.getIsActive());
    assertTrue(studentRepository.existsById(student.getId()));
    assertEquals(
        1, groupHistoryRepository.findByStudentIdOrderByStartDateDesc(student.getId()).size());
  }

  @Test
  void createStudent_duplicateEmail_returnsConflict() {
    var cohort = saveCohort();
    var group = saveGroup(cohort, "K1");
    saveStudent("dup@hei.school", cohort);

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Other")
                .firstName("Name")
                .email("dup@hei.school")
                .cohortId(cohort.getId())
                .initialGroupId(group.getId()),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void createStudent_unknownCohort_returnsNotFound() {
    var group = saveGroup(saveCohort(), "K1");

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Rakoto")
                .firstName("Jean")
                .email("jean.rakoto@hei.school")
                .cohortId(UUID.randomUUID())
                .initialGroupId(group.getId()),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void createStudent_groupOutsideCohort_returnsUnprocessable() {
    var otherCohort = saveCohort();
    var groupInOtherCohort = saveGroup(otherCohort, "K1");

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Rakoto")
                .firstName("Jean")
                .email("jean.rakoto@hei.school")
                .cohortId(saveCohort().getId())
                .initialGroupId(groupInOtherCohort.getId()),
            Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertEquals("UNPROCESSABLE_ENTITY", response.getBody().getCode());
  }

  @Test
  void updateStudent_updatesProvidedFields() {
    var cohort = saveCohort();
    var group = saveGroup(cohort, "K1");
    var existing = saveStudent("update.me@hei.school", cohort);
    saveOpenGroupHistory(existing, group);

    var response =
        patch(
            "/students/" + existing.getId(),
            new StudentUpdateRequest()
                .lastName("Updated")
                .firstName("Rina")
                .email("rina.updated@hei.school")
                .isActive(false),
            StudentResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var student = response.getBody();
    assertNotNull(student);
    assertEquals("Updated", student.getLastName());
    assertEquals("Rina", student.getFirstName());
    assertEquals("rina.updated@hei.school", student.getEmail());
    assertFalse(student.getIsActive());
    assertEquals(group.getId(), student.getCurrentGroup().getId());
  }

  @Test
  void updateStudent_unknownStudent_returnsNotFound() {
    var response =
        patch(
            "/students/" + UUID.randomUUID(),
            new StudentUpdateRequest().lastName("X"),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void updateStudent_duplicateEmail_returnsConflict() {
    var cohort = saveCohort();
    saveStudent("taken@hei.school", cohort);
    var other = saveStudent("free@hei.school", cohort);

    var response =
        patch(
            "/students/" + other.getId(),
            new StudentUpdateRequest().email("taken@hei.school"),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void listStudentGroupHistory_returnsChronologicalAssignments() {
    var cohort = saveCohort();
    var group1 = saveGroup(cohort, "K1");
    var group2 = saveGroup(cohort, "K2");
    var student = saveStudent("history@hei.school", cohort);
    saveGroupHistory(student, group1, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 1));
    saveGroupHistory(student, group2, LocalDate.of(2025, 6, 2), null);

    var response =
        restTemplate.getForEntity(
            "/students/" + student.getId() + "/group-history", StudentGroupHistoryResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var history = List.of(response.getBody());
    assertEquals(2, history.size());
    assertEquals("K1", history.get(0).getGroup().getReference());
    assertEquals("K2", history.get(1).getGroup().getReference());
    assertTrue(history.get(0).getStartDate().isBefore(history.get(1).getStartDate()));
    assertNull(history.get(1).getEndDate());
  }

  @Test
  void listStudentGroupHistory_unknownStudent_returnsNotFound() {
    var response =
        restTemplate.getForEntity("/students/" + UUID.randomUUID() + "/group-history", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void listStudentTrackHistory_returnsChronologicalAssignments() {
    var cohort = saveCohort();
    var trackEl = saveTrack("EL", "Electronique");
    var trackTn = saveTrack("TN", "Telecom");
    var student = saveStudent("track.history@hei.school", cohort);
    saveTrackHistory(student, trackEl, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 1));
    saveTrackHistory(student, trackTn, LocalDate.of(2025, 6, 2), null);

    var response =
        restTemplate.getForEntity(
            "/students/" + student.getId() + "/track-history", StudentTrackHistoryResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var history = List.of(response.getBody());
    assertEquals(2, history.size());
    assertEquals("EL", history.get(0).getTrack().getCode().toString());
    assertEquals("TN", history.get(1).getTrack().getCode().toString());
    assertTrue(history.get(0).getStartDate().isBefore(history.get(1).getStartDate()));
  }

  @Test
  void listStudentTranscripts_returnsPreviouslyGeneratedTranscripts() {
    var cohort = saveCohort();
    var student = saveStudent("transcripts@hei.school", cohort);
    inTransaction(
        () -> {
          var managedStudent = studentRepository.findById(student.getId()).orElseThrow();
          transcriptRepository.save(
              JTranscript.builder()
                  .student(managedStudent)
                  .type(TranscriptType.FULL)
                  .overallAverage(new BigDecimal("13.50"))
                  .creditsEarned(120)
                  .storageKey("students/" + student.getId() + "/full.pdf")
                  .recipientEmail("transcripts@hei.school")
                  .build());
          return null;
        });

    stubPresignedDownloadUrl(student);

    var response =
        restTemplate.getForEntity(
            "/students/" + student.getId() + "/transcripts", TranscriptResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var transcripts = List.of(response.getBody());
    assertEquals(1, transcripts.size());
    var transcript = transcripts.get(0);
    assertEquals("FULL", transcript.getType().toString());
    assertEquals(
        "https://dummy-bucket.s3.eu-west-3.amazonaws.com/students/" + student.getId() + "/full.pdf",
        transcript.getDownloadUrl());
    assertEquals(120, transcript.getCreditsEarned());
  }

  @Test
  void listStudentTranscripts_unknownStudent_returnsNotFound() {
    var response =
        restTemplate.getForEntity("/students/" + UUID.randomUUID() + "/transcripts", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  private JCohort saveCohort() {
    return cohortRepository.save(
        JCohort.builder().label("P14").entryYear(2024).expectedGraduationYear(2027).build());
  }

  @SneakyThrows
  private void stubPresignedDownloadUrl(JStudent student) {
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(
            new URL(
                "https://dummy-bucket.s3.eu-west-3.amazonaws.com/students/"
                    + student.getId()
                    + "/full.pdf"));
  }

  private JTrack saveTrack(String code, String label) {
    return trackRepository.save(
        JTrack.builder().code(app.mata.gradup.model.TrackCode.valueOf(code)).label(label).build());
  }

  private JGroup saveGroup(JCohort cohort, String reference) {
    return groupRepository.save(JGroup.builder().reference(reference).cohort(cohort).build());
  }

  private JStudent saveStudent(String email, JCohort cohort) {
    return inTransaction(
        () -> {
          var managedCohort = cohortRepository.findById(cohort.getId()).orElseThrow();
          var user =
              userRepository.save(
                  JUser.builder()
                      .lastName("Rakoto")
                      .firstName("Jean")
                      .email(email)
                      .passwordHash("hashed")
                      .role(Role.STUDENT)
                      .isActive(true)
                      .build());
          return studentRepository.save(
              JStudent.builder().user(user).cohort(managedCohort).build());
        });
  }

  private void saveOpenGroupHistory(JStudent student, JGroup group) {
    saveGroupHistory(student, group, LocalDate.now(), null);
  }

  private void saveGroupHistory(
      JStudent student, JGroup group, LocalDate startDate, LocalDate endDate) {
    inTransaction(
        () -> {
          var managedStudent = studentRepository.findById(student.getId()).orElseThrow();
          var managedGroup = groupRepository.findById(group.getId()).orElseThrow();
          groupHistoryRepository.save(
              JStudentGroupHistory.builder()
                  .student(managedStudent)
                  .group(managedGroup)
                  .startDate(startDate)
                  .endDate(endDate)
                  .build());
          return null;
        });
  }

  private void saveTrackHistory(
      JStudent student, JTrack track, LocalDate startDate, LocalDate endDate) {
    inTransaction(
        () -> {
          var managedStudent = studentRepository.findById(student.getId()).orElseThrow();
          var managedTrack = trackRepository.findById(track.getId()).orElseThrow();
          trackHistoryRepository.save(
              JStudentTrackHistory.builder()
                  .student(managedStudent)
                  .track(managedTrack)
                  .startDate(startDate)
                  .endDate(endDate)
                  .build());
          return null;
        });
  }

  private <T> T inTransaction(java.util.function.Supplier<T> action) {
    return new TransactionTemplate(transactionManager).execute(status -> action.get());
  }

  private <T> ResponseEntity<T> patch(String url, Object body, Class<T> responseType) {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    return restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body), responseType);
  }
}
