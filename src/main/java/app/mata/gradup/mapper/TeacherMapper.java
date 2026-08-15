package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.CourseSummary;
import app.mata.gradup.endpoint.rest.model.GroupSummary;
import app.mata.gradup.endpoint.rest.model.SemesterSummary;
import app.mata.gradup.endpoint.rest.model.TeacherResponse;
import app.mata.gradup.endpoint.rest.model.TeacherSummary;
import app.mata.gradup.model.Teacher;
import app.mata.gradup.model.User;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JTeacher;
import app.mata.gradup.repository.model.JTeacherAssignment;
import app.mata.gradup.repository.model.JUser;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TeacherMapper {

  @Mapping(source = "entity.user", target = "user")
  Teacher toDomain(JTeacher entity);

  @Mapping(source = "user.id", target = "id")
  @Mapping(source = "user.lastName", target = "lastName")
  @Mapping(source = "user.firstName", target = "firstName")
  @Mapping(source = "user.email", target = "email")
  @Mapping(source = "user.reference", target = "reference")
  TeacherResponse toRest(Teacher domain);

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

  default User toUser(JUser entity) {
    return entity == null
        ? null
        : new User(
            entity.getId(),
            entity.getReference(),
            entity.getLastName(),
            entity.getFirstName(),
            entity.getEmail(),
            entity.getPhone(),
            entity.getRole(),
            entity.getIsActive());
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

  default <T> T nullableOrNull(JsonNullable<T> value) {
    return value == null ? null : value.orElse(null);
  }
}
