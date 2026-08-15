package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.CourseSummary;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.endpoint.rest.model.GroupSummary;
import app.mata.gradup.endpoint.rest.model.SemesterSummary;
import app.mata.gradup.endpoint.rest.model.TeacherSummary;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JTeacher;
import app.mata.gradup.repository.model.JTeacherAssignment;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseOfferingMapper {

  DateTimeFormatter EXAM_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  default CourseOfferingResponse toRest(
      JCourseOffering offering, List<JTeacherAssignment> assignments) {
    return new CourseOfferingResponse()
        .id(offering.getId())
        .course(toCourseSummary(offering.getCourse()))
        .group(toGroupSummary(offering.getGroup()))
        .semester(toSemesterSummary(offering.getSemester()))
        .teachers(assignments.stream().map(a -> toTeacherSummary(a.getTeacher())).toList())
        .gradingFinalized(offering.getGradingFinalized());
  }

  default ExamResponse toRest(JExam exam) {
    return new ExamResponse()
        .id(exam.getId())
        .offeringId(exam.getOffering().getId())
        .label(exam.getLabel())
        .examDate(exam.getExamDate())
        .examTime(exam.getExamTime() == null ? null : exam.getExamTime().format(EXAM_TIME_FORMATTER))
        .weightNumerator(exam.getWeightNumerator())
        .weightDenominator(exam.getWeightDenominator());
  }

  default CourseSummary toCourseSummary(JCourse course) {
    return course == null
        ? null
        : new CourseSummary()
            .id(course.getId())
            .reference(course.getReference())
            .title(course.getTitle())
            .credits(course.getCredits());
  }

  default GroupSummary toGroupSummary(JGroup group) {
    return group == null
        ? null
        : new GroupSummary().id(group.getId()).reference(group.getReference());
  }

  default SemesterSummary toSemesterSummary(JSemester semester) {
    return semester == null
        ? null
        : new SemesterSummary()
            .id(semester.getId())
            .number(semester.getNumber())
            .academicYearLabel(semester.getAcademicYear().getLabel());
  }

  default TeacherSummary toTeacherSummary(JTeacher teacher) {
    return teacher == null
        ? null
        : new TeacherSummary()
            .id(teacher.getId())
            .lastName(teacher.getUser().getLastName())
            .firstName(teacher.getUser().getFirstName());
  }
}
