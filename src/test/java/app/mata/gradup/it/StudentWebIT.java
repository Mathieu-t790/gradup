package app.mata.gradup.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.conf.SecuredFacadeIT;
import app.mata.gradup.conf.TestDataSeeder;
import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

class StudentWebIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TestDataSeeder seeder;

  @BeforeEach
  void setUp() {
    useCookieAwareClient(restTemplate);
    seeder.cleanDatabase();
  }

  @Test
  void student_sees_own_grades_with_semester_filter() {
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
    var el = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var group = seeder.group("G1", cohort, el);
    var year =
        seeder.academicYear("2021-2024", LocalDate.of(2021, 9, 1), LocalDate.of(2024, 7, 31));
    var s1 = seeder.semester(1, year, LocalDate.of(2021, 9, 1), LocalDate.of(2022, 1, 31));
    var s2 = seeder.semester(2, year, LocalDate.of(2022, 2, 1), LocalDate.of(2022, 6, 30));
    var prog1 = seeder.course("PROG1", 60, 1, null);
    var prog2 = seeder.course("PROG2", 60, 2, null);
    var exam1 = seeder.exam(seeder.offering(prog1, group, s1));
    var exam2 = seeder.exam(seeder.offering(prog2, group, s2));
    var student = seeder.student("STD21001", "Rakoto", "Hery", "hery@cu.te", cohort, el, group);
    seeder.grade(student, exam1, "12.00");
    seeder.grade(student, exam2, "9.00");

    loginAsUser(restTemplate, "hery@cu.te");

    var response = restTemplate.getForEntity("/student/grades", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("PROG1"));
    assertTrue(response.getBody().contains("PROG2"));
    assertTrue(response.getBody().contains("12.0"));
    assertTrue(response.getBody().contains("9.0"));

    var filtered =
        restTemplate.getForEntity("/student/grades?semesterId=" + s1.getId(), String.class);

    assertEquals(HttpStatus.OK, filtered.getStatusCode());
    assertNotNull(filtered.getBody());
    assertTrue(filtered.getBody().contains("PROG1"));
    assertTrue(!filtered.getBody().contains("PROG2"));
  }

  @Test
  void student_without_grades_sees_empty_state() {
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
    var el = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var group = seeder.group("G1", cohort, el);
    seeder.student("STD21001", "Rakoto", "Hery", "hery@cu.te", cohort, el, group);

    loginAsUser(restTemplate, "hery@cu.te");

    var response = restTemplate.getForEntity("/student/grades", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("Aucune note"));
  }

  @Test
  void student_home_redirects_to_grades() {
    var cohort = seeder.cohort("Mpamakilay", 2021, 2024);
    var el = seeder.track(TrackCode.EL, "Ecosysteme Logiciel");
    var group = seeder.group("G1", cohort, el);
    seeder.student("STD21001", "Rakoto", "Hery", "hery@cu.te", cohort, el, group);
    loginAsUser(restTemplate, "hery@cu.te");

    var response = restTemplate.getForEntity("/", String.class);

    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    assertTrue(response.getHeaders().getLocation().toString().contains("/student/grades"));
  }

  @Test
  void student_grades_page_is_forbidden_for_admin() {
    seedUser("admin.web@cu.te", Role.ADMIN);
    loginAsUser(restTemplate, "admin.web@cu.te");

    var response = restTemplate.getForEntity("/student/grades", String.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void student_grades_page_is_forbidden_for_teacher() {
    seeder.teacher("teacher.web@cu.te", "Rakoto", "Tia");
    loginAsUser(restTemplate, "teacher.web@cu.te");

    var response = restTemplate.getForEntity("/student/grades", String.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }
}
