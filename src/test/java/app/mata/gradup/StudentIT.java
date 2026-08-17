package app.mata.gradup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.StudentCreateRequest;
import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentResponse;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentUpdateRequest;
import app.mata.gradup.mail.Email;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTrack;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class StudentIT extends SecuredFacadeIT {

  private static final Pattern RANDOM_PASSWORD = Pattern.compile("\\b[0-9a-f]{32}\\b");

  @MockBean private Mailer mailer;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private UserRepository userRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private StudentGroupHistoryRepository groupHistoryRepository;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void createStudent_returnsCreatedStudentWithServerGeneratedFields() {
    var cohort = saveCohort();
    var group = saveGroup(cohort, "K1");

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te")
                .cohortId(cohort.getId())
                .initialGroupId(group.getId()),
            StudentResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var student = response.getBody();
    assertNotNull(student);
    assertNotNull(student.getId());
    assertEquals("Mathieu", student.getLastName());
    assertEquals("Tafita", student.getFirstName());
    assertEquals("tafita@cu.te", student.getEmail());
    assertNotNull(student.getReference());
    assertTrue(student.getReference().startsWith("STD"));
    assertEquals(cohort.getId(), student.getCohort().getId());
    assertEquals(group.getId(), Objects.requireNonNull(student.getCurrentGroup()).getId());
    assertNull(student.getCurrentTrack());
    assertNotNull(student.getEnrollmentDate());
    assertTrue(student.getIsActive());
    assertTrue(studentRepository.existsById(student.getId()));
    assertEquals(
        1, groupHistoryRepository.findByStudentIdOrderByStartDateDesc(student.getId()).size());
  }

  @Test
  void createStudent_sendsCredentialsEmailWithBcryptHash() {
    var cohort = saveCohort();
    var group = saveGroup(cohort, "K1");

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te")
                .cohortId(cohort.getId())
                .initialGroupId(group.getId()),
            StudentResponse.class);

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    var email = emailCaptor.getValue();
    var student = response.getBody();

    assertEquals("tafita@cu.te", email.to().getAddress());
    assertTrue(email.subject().contains("Vos identifiants"));
    assertNotNull(student);
    assertTrue(email.subject().contains(student.getReference()));
    assertTrue(email.htmlBody().contains("Tafita"));
    assertTrue(email.htmlBody().contains("tafita@cu.te"));
    assertTrue(email.htmlBody().contains("data:image/png"));

    var rawPassword =
        RANDOM_PASSWORD.matcher(email.htmlBody()).results().findFirst().orElseThrow().group();
    var savedUser = userRepository.findByEmail("tafita@cu.te").orElseThrow();
    assertTrue(passwordEncoder.matches(rawPassword, savedUser.getPasswordHash()));
  }

  @Test
  void createStudent_duplicateEmail_returnsConflict() {
    var cohort = saveCohort();
    var group = saveGroup(cohort, "K1");
    saveStudent("tafita@cu.te", cohort);

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te")
                .cohortId(cohort.getId())
                .initialGroupId(group.getId()),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void createStudent_unknownCohort_returnsNotFound() {
    var group = saveGroup(saveCohort(), "K1");

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te")
                .cohortId(UUID.randomUUID())
                .initialGroupId(group.getId()),
            Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void createStudent_groupOutsideCohort_returnsUnprocessable() {
    var otherCohort = seeder.cohort("Tohindia", 2022, 2025);
    var groupInOtherCohort = saveGroup(otherCohort, "K1");

    var response =
        restTemplate.postForEntity(
            "/students",
            new StudentCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te")
                .cohortId(saveCohort().getId())
                .initialGroupId(groupInOtherCohort.getId()),
            Error.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UNPROCESSABLE_ENTITY", response.getBody().getCode());
  }

  @Test
  void updateStudent_updatesProvidedFields() {
    var cohort = saveCohort();
    var group = saveGroup(cohort, "K1");
    var existing = saveStudent("tafita@cu.te", cohort);
    saveOpenGroupHistory(existing, group);

    var response =
        patch(
            "/students/" + existing.getId(),
            new StudentUpdateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita.mathieu@cu.te")
                .isActive(false),
            StudentResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var student = response.getBody();
    assertNotNull(student);
    assertEquals("Mathieu", student.getLastName());
    assertEquals("Tafita", student.getFirstName());
    assertEquals("tafita.mathieu@cu.te", student.getEmail());
    assertFalse(student.getIsActive());
    assertEquals(group.getId(), Objects.requireNonNull(student.getCurrentGroup()).getId());
  }

  @Test
  void updateStudent_updatesDateOfBirth() {
    var cohort = saveCohort();
    var student = saveStudent("tafita@cu.te", cohort);

    var response =
        patch(
            "/students/" + student.getId(),
            new StudentUpdateRequest().dateOfBirth(LocalDate.of(2002, 5, 20)),
            StudentResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(LocalDate.of(2002, 5, 20), response.getBody().getDateOfBirth());
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
    saveStudent("taken@cu.te", cohort);
    var other = saveStudent("free@cu.te", cohort);

    var response =
        patch(
            "/students/" + other.getId(),
            new StudentUpdateRequest().email("taken@cu.te"),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void listStudentGroupHistory_returnsChronologicalAssignments() {
    var cohort = saveCohort();
    var group1 = saveGroup(cohort, "K1");
    var group2 = saveGroup(cohort, "K2");
    var student = saveStudent("tafita@cu.te", cohort);
    saveGroupHistory(student, group1, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 1));
    saveGroupHistory(student, group2, LocalDate.of(2025, 6, 2), null);

    var response =
        restTemplate.getForEntity(
            "/students/" + student.getId() + "/group-history", StudentGroupHistoryResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
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
    var trackEl = seeder.track(TrackCode.EL, "Electronique");
    var trackTn = seeder.track(TrackCode.TN, "Telecom");
    var student = saveStudent("tafita@cu.te", cohort);
    saveTrackHistory(student, trackEl, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 1));
    saveTrackHistory(student, trackTn, LocalDate.of(2025, 6, 2), null);

    var response =
        restTemplate.getForEntity(
            "/students/" + student.getId() + "/track-history", StudentTrackHistoryResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var history = List.of(response.getBody());
    assertEquals(2, history.size());
    assertEquals("EL", history.get(0).getTrack().getCode().toString());
    assertEquals("TN", history.get(1).getTrack().getCode().toString());
    assertTrue(history.get(0).getStartDate().isBefore(history.get(1).getStartDate()));
  }

  private JCohort saveCohort() {
    return seeder.cohort("Mpamakilay", 2021, 2024);
  }

  private JGroup saveGroup(JCohort cohort, String reference) {
    return seeder.group(reference, cohort, null);
  }

  private JStudent saveStudent(String email, JCohort cohort) {
    return seeder.studentWithoutHistories(email, "Mathieu", "Tafita", email, cohort);
  }

  private void saveOpenGroupHistory(JStudent student, JGroup group) {
    seeder.addGroupHistory(student, group, LocalDate.now(), null);
  }

  private void saveGroupHistory(
      JStudent student, JGroup group, LocalDate startDate, LocalDate endDate) {
    seeder.addGroupHistory(student, group, startDate, endDate);
  }

  private void saveTrackHistory(
      JStudent student, JTrack track, LocalDate startDate, LocalDate endDate) {
    seeder.addTrackHistory(student, track, startDate, endDate);
  }

  private <T> ResponseEntity<T> patch(String url, Object body, Class<T> responseType) {
    return restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body), responseType);
  }
}
