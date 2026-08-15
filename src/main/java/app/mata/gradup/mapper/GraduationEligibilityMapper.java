package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CourseSummary;
import app.mata.gradup.endpoint.rest.model.GraduationEligibilityResponse;
import app.mata.gradup.endpoint.rest.model.GraduationEligibilityResponseFailingCoursesInner;
import app.mata.gradup.model.Course;
import app.mata.gradup.model.FailingCourse;
import app.mata.gradup.model.GraduationEligibility;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = StudentMapper.class)
public interface GraduationEligibilityMapper {

  GraduationEligibilityResponse toRest(GraduationEligibility domain);

  GraduationEligibilityResponseFailingCoursesInner toRestFailing(FailingCourse failingCourse);

  CourseSummary toCourseSummary(Course course);
}
