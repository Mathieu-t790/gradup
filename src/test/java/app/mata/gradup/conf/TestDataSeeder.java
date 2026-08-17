package app.mata.gradup.conf;

import app.mata.gradup.model.Role;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.DiplomaRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GradeDisputeRepository;
import app.mata.gradup.repository.GradeHistoryRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.SemesterCreditValidationRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.StudentGroupHistoryRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.TeacherRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.TranscriptDetailRepository;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TestDataSeeder {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private GradeDisputeRepository gradeDisputeRepository;
  @Autowired private GradeHistoryRepository gradeHistoryRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private TranscriptDetailRepository transcriptDetailRepository;
  @Autowired private TranscriptRepository transcriptRepository;
  @Autowired private DiplomaRepository diplomaRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private TeacherAssignmentRepository teacherAssignmentRepository;
  @Autowired private CourseOfferingRepository courseOfferingRepository;
  @Autowired private StudentTrackHistoryRepository studentTrackHistoryRepository;
  @Autowired private StudentGroupHistoryRepository studentGroupHistoryRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private SemesterCreditValidationRepository semesterCreditValidationRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private AcademicYearRepository academicYearRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private TrackRepository trackRepository;
  @Autowired private CohortRepository cohortRepository;
  @Autowired private UserRepository userRepository;

  public void cleanDatabase() {
    gradeDisputeRepository.deleteAll();
    gradeHistoryRepository.deleteAll();
    gradeRepository.deleteAll();
    transcriptDetailRepository.deleteAll();
    transcriptRepository.deleteAll();
    diplomaRepository.deleteAll();
    examRepository.deleteAll();
    teacherAssignmentRepository.deleteAll();
    courseOfferingRepository.deleteAll();
    studentTrackHistoryRepository.deleteAll();
    studentGroupHistoryRepository.deleteAll();
    studentRepository.deleteAll();
    courseRepository.deleteAll();
    semesterCreditValidationRepository.deleteAll();
    semesterRepository.deleteAll();
    groupRepository.deleteAll();
    academicYearRepository.deleteAll();
    teacherRepository.deleteAll();
    trackRepository.deleteAll();
    cohortRepository.deleteAll();
    userRepository.deleteAll();
  }

  public JCohort cohort(String label, int entryYear, int expectedGraduationYear) {
    return cohortRepository.save(
        JCohort.builder()
            .label(label)
            .entryYear(entryYear)
            .expectedGraduationYear(expectedGraduationYear)
            .build());
  }

  public JTrack track(TrackCode code, String label) {
    return trackRepository.save(JTrack.builder().code(code).label(label).build());
  }

  public JTrack track(String code, String label) {
    return track(TrackCode.valueOf(code), label);
  }

  public JGroup group(String reference, JCohort cohort, JTrack track) {
    return groupRepository.save(
        JGroup.builder().reference(reference).cohort(cohort).track(track).build());
  }

  public JAcademicYear academicYear(String label, LocalDate startDate, LocalDate endDate) {
    return academicYearRepository.save(
        JAcademicYear.builder().label(label).startDate(startDate).endDate(endDate).build());
  }

  public JSemester semester(
      int number, JAcademicYear year, LocalDate startDate, LocalDate endDate) {
    return semesterRepository.save(
        JSemester.builder()
            .number(number)
            .academicYear(year)
            .startDate(startDate)
            .endDate(endDate)
            .build());
  }

  public JStudent student(
      String reference,
      String lastName,
      String firstName,
      String email,
      JCohort cohort,
      JTrack track,
      JGroup group) {
    return student(
        reference, lastName, firstName, email, cohort, track, group, LocalDate.of(2021, 9, 1));
  }

  public JStudent student(
      String reference,
      String lastName,
      String firstName,
      String email,
      JCohort cohort,
      JTrack track,
      JGroup group,
      LocalDate startDate) {
    UUID userId = UUID.randomUUID();
    jdbcTemplate.update(
        """
INSERT INTO users (user_id, reference, last_name, first_name, email, password_hash, role, is_active)
VALUES (?, ?, ?, ?, ?, ?, ?, ?)
""",
        userId,
        reference,
        lastName,
        firstName,
        email,
        "hashed",
        Role.STUDENT.name(),
        true);
    JUser user = userRepository.findById(userId).orElseThrow();
    JStudent student = studentRepository.save(JStudent.builder().user(user).cohort(cohort).build());
    studentTrackHistoryRepository.save(
        JStudentTrackHistory.builder()
            .student(student)
            .track(track)
            .startDate(startDate)
            .endDate(null)
            .build());
    studentGroupHistoryRepository.save(
        JStudentGroupHistory.builder()
            .student(student)
            .group(group)
            .startDate(startDate)
            .endDate(null)
            .build());
    return student;
  }

  public JCourse course(String reference, int credits, int semesterNumber, JTrack track) {
    return courseRepository.save(
        JCourse.builder()
            .reference(reference)
            .title(reference)
            .credits(credits)
            .semesterNumber(semesterNumber)
            .track(track)
            .build());
  }

  public JCourseOffering offering(JCourse course, JGroup group, JSemester semester) {
    return courseOfferingRepository.save(
        JCourseOffering.builder()
            .course(course)
            .group(group)
            .semester(semester)
            .gradingFinalized(true)
            .build());
  }

  public JExam exam(JCourseOffering offering) {
    return examRepository.save(
        JExam.builder()
            .offering(offering)
            .label("Final")
            .examDate(LocalDate.of(2022, 5, 30))
            .weightNumerator(1)
            .weightDenominator(1)
            .build());
  }

  public JTeacher teacher(String email, String lastName, String firstName) {
    JUser user =
        userRepository.save(
            JUser.builder()
                .reference("TEA-" + email)
                .lastName(lastName)
                .firstName(firstName)
                .email(email)
                .passwordHash("hashed")
                .role(Role.TEACHER)
                .isActive(true)
                .build());
    return teacherRepository.save(JTeacher.builder().user(user).specialty(null).build());
  }

  public void teacherAssignment(JTeacher teacher, JCourseOffering offering) {
    JTeacher managedTeacher = teacherRepository.findById(teacher.getId()).orElseThrow();
    JCourseOffering managedOffering =
        courseOfferingRepository.findById(offering.getId()).orElseThrow();
    teacherAssignmentRepository.save(
        JTeacherAssignment.builder().offering(managedOffering).teacher(managedTeacher).build());
  }

  public JGradeDispute dispute(JGrade grade, JStudent student, String reason) {
    return gradeDisputeRepository.save(
        JGradeDispute.builder().grade(grade).student(student).reason(reason).build());
  }

  public DisputeScenario disputeScenario() {
    return inTransaction(
        () -> {
          var year = academicYear("2024-2025", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 8, 31));
          var semester = semester(1, year, LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 31));
          var cohort = cohort("Mpamakilay", 2021, 2024);
          var track = track(TrackCode.EL, "Ecosysteme Logiciel");
          var group = group("K1", cohort, track);
          var course = course("PROG1", 6, 1, track);
          var offering = offering(course, group, semester);
          var exam = exam(offering);
          return new DisputeScenario(cohort, track, group, semester, offering, exam);
        });
  }

  public record DisputeScenario(
      JCohort cohort,
      JTrack track,
      JGroup group,
      JSemester semester,
      JCourseOffering offering,
      JExam exam) {}

  public void grade(JStudent student, JExam exam, String score) {
    gradeRepository.save(
        JGrade.builder()
            .student(student)
            .exam(exam)
            .score(new BigDecimal(score))
            .recordedAt(Instant.now())
            .recordedBy(adminUserId())
            .build());
  }

  public void changeScore(String reference, String courseReference, BigDecimal score) {
    jdbcTemplate.update(
        """
        UPDATE grade g
        SET score = ?
        FROM exam ex
        JOIN course_offering co ON co.offering_id = ex.offering_id
        JOIN course c ON c.course_id = co.course_id
        WHERE ex.exam_id = g.exam_id
          AND c.reference = ?
          AND g.student_id IN (
            SELECT s.user_id
            FROM student s
            JOIN users u ON u.user_id = s.user_id
            WHERE u.reference = ?
          )
        """,
        score,
        courseReference,
        reference);
  }

  public byte[] goldenFile(String resource) throws IOException {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("missing golden file " + resource);
      }
      return in.readAllBytes();
    }
  }

  public <T> T inTransaction(java.util.function.Supplier<T> action) {
    return transactionTemplate.execute(status -> action.get());
  }

  public UUID adminUserId() {
    return userRepository.findByEmail(SecuredFacadeIT.ADMIN_EMAIL).orElseThrow().getId();
  }
}
