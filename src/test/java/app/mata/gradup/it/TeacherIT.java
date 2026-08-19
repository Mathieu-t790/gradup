package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.Error;
import app.mata.gradup.endpoint.rest.model.TeacherCreateRequest;
import app.mata.gradup.endpoint.rest.model.TeacherResponse;
import app.mata.gradup.mail.Email;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.TeacherRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JTeacher;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

class TeacherIT extends SecuredFacadeIT {

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private UserRepository userRepository;

  @MockBean private Mailer mailer;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
    loginAsAdmin(restTemplate);
  }

  @Test
  void listTeachers_returnsEmptyList_whenNone() {
    var response = restTemplate.getForEntity("/teachers", TeacherResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().length);
  }

  @Test
  void listTeachers_returnsAllTeachers() {
    seeder.teacher("tafita@cu.te", "Mathieu", "Tafita", "Mathematiques");
    seeder.teacher("rindra@cu.te", "Rindra", "Andry", "Programmation");

    var response = restTemplate.getForEntity("/teachers", TeacherResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    var teachers = List.of(response.getBody());
    assertEquals(2, teachers.size());
    assertTrue(teachers.stream().anyMatch(t -> t.getEmail().equals("tafita@cu.te")));
    assertTrue(teachers.stream().anyMatch(t -> t.getEmail().equals("rindra@cu.te")));

    var tafita =
        teachers.stream()
            .filter(t -> t.getEmail().equals("tafita@cu.te"))
            .findFirst()
            .orElseThrow();
    assertEquals("Tafita", tafita.getFirstName());
    assertEquals("Mathematiques", tafita.getSpecialty());
    assertNotNull(tafita.getReference());
    assertTrue(tafita.getReference().startsWith("TCH"));
  }

  @Test
  void createTeacher_returnsCreatedTeacherWithServerGeneratedFields() {
    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te")
                .specialty("Mathematiques"),
            TeacherResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var teacher = response.getBody();
    assertNotNull(teacher);
    assertNotNull(teacher.getId());
    assertEquals("Mathieu", teacher.getLastName());
    assertEquals("Tafita", teacher.getFirstName());
    assertEquals("tafita@cu.te", teacher.getEmail());
    assertNotNull(teacher.getReference());
    assertTrue(teacher.getReference().startsWith("TCH"));
    assertEquals("Mathematiques", teacher.getSpecialty());
    assertTrue(teacherRepository.existsById(teacher.getId()));

    var savedUser = userRepository.findByEmail("tafita@cu.te").orElseThrow();
    assertEquals(Role.TEACHER, savedUser.getRole());
    assertEquals(teacher.getReference(), savedUser.getReference());
  }

  @Test
  void createTeacher_optionalSpecialtyCanBeNull() {
    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te"),
            TeacherResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var teacher = response.getBody();
    assertNotNull(teacher);
    assertNull(teacher.getSpecialty());
  }

  @Test
  void createTeacher_sendsCredentialsEmail() {
    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("tafita@cu.te"),
            TeacherResponse.class);

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    var email = emailCaptor.getValue();
    var teacher = response.getBody();

    assertEquals("tafita@cu.te", email.to().getAddress());
    assertNotNull(teacher);
    assertTrue(email.subject().contains(teacher.getReference()));
    assertTrue(email.htmlBody().contains("Tafita"));
    assertTrue(email.htmlBody().contains("tafita@cu.te"));
  }

  @Test
  void createTeacher_duplicateEmail_returnsConflict() {
    seeder.teacher("taken@cu.te", "Mathieu", "Tafita", "Mathematiques");

    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest().lastName("Mathieu").firstName("Tafita").email("taken@cu.te"),
            Error.class);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("CONFLICT", response.getBody().getCode());
  }

  @Test
  void createTeacher_blankLastName_returnsBadRequest() {
    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest().lastName(" ").firstName("Tafita").email("tafita@cu.te"),
            Error.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
  }

  @Test
  void createTeacher_teacherRole_cannotCreate() {
    seedUser("teacher@cu.te", Role.TEACHER);
    loginAs(restTemplate, "teacher@cu.te");

    var response =
        restTemplate.postForEntity(
            "/teachers",
            new TeacherCreateRequest()
                .lastName("Mathieu")
                .firstName("Tafita")
                .email("another@cu.te"),
            Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void listTeacherCourseOfferings_returnsAssignedOfferings() {
    var fixture = seedTeacherWithOffering();

    var response =
        restTemplate.getForEntity(
            "/teachers/" + fixture.teacher.getId() + "/course-offerings",
            CourseOfferingResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().length);
    var courseOffering = response.getBody()[0];
    assertEquals(fixture.offering.getId(), courseOffering.getId());
    assertEquals("Pro1", courseOffering.getCourse().getReference());
    assertEquals("K1", courseOffering.getGroup().getReference());
    assertEquals(1, courseOffering.getSemester().getNumber());
    assertEquals("2024-2025", courseOffering.getSemester().getAcademicYearLabel());
    assertEquals(1, courseOffering.getTeachers().size());
    assertEquals("Tafita", courseOffering.getTeachers().getFirst().getFirstName());
    assertFalse(courseOffering.getGradingFinalized());
  }

  @Test
  void listTeacherCourseOfferings_unknownTeacher_returnsNotFound() {
    var response =
        restTemplate.getForEntity(
            "/teachers/" + UUID.randomUUID() + "/course-offerings", Error.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("NOT_FOUND", response.getBody().getCode());
  }

  @Test
  void listTeachers_teacherRole_returnsForbidden() {
    seedUser("teacher@cu.te", Role.TEACHER);
    loginAs(restTemplate, "teacher@cu.te");

    var response = restTemplate.getForEntity("/teachers", Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void listTeacherCourseOfferings_studentRole_returnsForbidden() {
    seedUser("student.web@cu.te", Role.STUDENT);
    loginAs(restTemplate, "student.web@cu.te");

    var response =
        restTemplate.getForEntity(
            "/teachers/" + UUID.randomUUID() + "/course-offerings", Error.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("FORBIDDEN", response.getBody().getCode());
  }

  @Test
  void listTeacherCourseOfferings_teacherRole_returnsOwnOfferings() {
    var fixture = seedTeacherWithOffering();
    loginAsUser(restTemplate, "tafita@cu.te");

    var response =
        restTemplate.getForEntity(
            "/teachers/" + fixture.teacher.getId() + "/course-offerings",
            CourseOfferingResponse[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().length);
    assertEquals(fixture.offering.getId(), response.getBody()[0].getId());
  }

  private TeacherWithOffering seedTeacherWithOffering() {
    var teacher =
        seeder.inTransaction(
            () -> seeder.teacher("tafita@cu.te", "Mathieu", "Tafita", "Mathematiques"));
    var offering =
        seeder.inTransaction(
            () -> {
              var year =
                  seeder.academicYear(
                      "2024-2025", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 8, 31));
              var semester =
                  seeder.semester(1, year, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 31));
              var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
              var track = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
              var group = seeder.group("K1", cohort, track);
              var course = seeder.course("Pro1", "Programmation", 5, 1, track);
              return seeder.offering(course, group, semester, false);
            });
    seeder.inTransaction(
        () -> {
          seeder.teacherAssignment(teacher, offering);
          return null;
        });
    return new TeacherWithOffering(teacher, offering);
  }

  private record TeacherWithOffering(JTeacher teacher, JCourseOffering offering) {}
}
