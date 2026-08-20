package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.endpoint.rest.model.GradeCreateRequest;
import app.mata.gradup.endpoint.rest.model.GradeResponse;
import app.mata.gradup.endpoint.rest.model.GradeUpdateRequest;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.service.utils.Users;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TeacherViewService {

  private static final int PAGE_SIZE = 200;

  private final TeacherService teacherService;
  private final CourseOfferingService courseOfferingService;
  private final GradeService gradeService;
  private final StudentRepository studentRepository;
  private final TeacherAssignmentRepository teacherAssignmentRepository;
  private final GradeRepository gradeRepository;

  @Transactional(readOnly = true)
  public TeacherCoursesView teacherCourses(UUID teacherId) {
    var offerings = teacherService.listTeacherCourseOfferings(teacherId);
    var semesters =
        offerings.stream()
            .map(
                offering ->
                    new SemesterOption(
                        offering.getSemester().getId(),
                        offering.getSemester().getNumber(),
                        offering.getSemester().getAcademicYearLabel()))
            .distinct()
            .toList();
    return new TeacherCoursesView(offerings, semesters);
  }

  @Transactional(readOnly = true)
  public TeacherOfferingView teacherOffering(UUID teacherId, UUID offeringId) {
    requireAssigned(teacherId, offeringId);
    var offering = courseOfferingService.getCourseOffering(offeringId);
    var exams =
        courseOfferingService.listOfferingExams(offeringId).stream()
            .map(
                exam ->
                    new ExamView(exam.getId(), exam.getLabel(), studentsForExam(offering, exam)))
            .toList();
    return new TeacherOfferingView(
        offeringId,
        offering.getCourse().getReference(),
        offering.getCourse().getTitle(),
        offering.getGroup().getReference(),
        semesterLabel(offering),
        exams);
  }

  @Transactional(readOnly = true)
  public UUID offeringIdForGrade(UUID teacherId, UUID gradeId) {
    var offeringId =
        gradeRepository
            .findById(gradeId)
            .orElseThrow(() -> new NotFoundException("Grade not found"))
            .getExam()
            .getOffering()
            .getId();
    requireAssigned(teacherId, offeringId);
    return offeringId;
  }

  @Transactional
  public void recordGrade(
      UUID teacherId, UUID offeringId, UUID examId, UUID studentId, Double score) {
    requireAssigned(teacherId, offeringId);
    gradeService.recordGrade(
        examId, new GradeCreateRequest().studentId(studentId).score(score), teacherId);
  }

  @Transactional
  public UUID updateGrade(UUID teacherId, UUID gradeId, Double score, String reason) {
    UUID offeringId = offeringIdForGrade(teacherId, gradeId);
    gradeService.updateGrade(
        gradeId, new GradeUpdateRequest().score(score).reason(reason), teacherId);
    return offeringId;
  }

  private List<StudentGradeRow> studentsForExam(
      CourseOfferingResponse offering, ExamResponse exam) {
    var students =
        studentRepository
            .findByCurrentGroupId(offering.getGroup().getId(), PageRequest.of(0, PAGE_SIZE))
            .getContent();
    var gradesByStudent =
        gradeService.listExamGrades(exam.getId()).stream()
            .collect(Collectors.toMap(GradeResponse::getStudentId, grade -> grade));
    return students.stream()
        .map(
            student -> {
              var grade = gradesByStudent.get(student.getId());
              return new StudentGradeRow(
                  student.getId(),
                  student.getUser().getReference(),
                  Users.fullName(student.getUser()),
                  grade == null ? null : grade.getId(),
                  grade == null ? null : grade.getScore());
            })
        .toList();
  }

  private static String semesterLabel(CourseOfferingResponse offering) {
    var semester = offering.getSemester();
    return semester.getNumber() + " - " + semester.getAcademicYearLabel();
  }

  private void requireAssigned(UUID teacherId, UUID offeringId) {
    if (!teacherAssignmentRepository.existsByTeacherIdAndOfferingId(teacherId, offeringId)) {
      throw new AccessDeniedException("Teacher is not assigned to this course offering");
    }
  }

  public record TeacherCoursesView(
      List<CourseOfferingResponse> offerings, List<SemesterOption> semesters) {}

  public record SemesterOption(UUID id, Integer number, String academicYearLabel) {}

  public record TeacherOfferingView(
      UUID offeringId,
      String courseReference,
      String courseTitle,
      String groupReference,
      String semesterLabel,
      List<ExamView> exams) {}

  public record ExamView(UUID examId, String label, List<StudentGradeRow> students) {}

  public record StudentGradeRow(
      UUID studentId, String reference, String fullName, UUID gradeId, Double score) {}
}
