package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.AcademicYearResponse;
import app.mata.gradup.endpoint.rest.model.SemesterCreditValidationResponse;
import app.mata.gradup.endpoint.rest.model.SemesterResponse;
import app.mata.gradup.model.AcademicYear;
import app.mata.gradup.model.Semester;
import app.mata.gradup.model.SemesterCreditValidation;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JSemesterCreditValidation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = ReferenceMapper.class)
public interface SemesterMapper {

  @Mapping(source = "entity.id", target = "id")
  @Mapping(source = "entity.semester", target = "semester", qualifiedByName = "toSemester")
  @Mapping(source = "entity.track", target = "track")
  @Mapping(source = "entity.totalCredits", target = "totalCredits")
  @Mapping(source = "entity.validatedAt", target = "validatedAt")
  @Mapping(source = "validatedByName", target = "validatedByName")
  SemesterCreditValidation toDomain(JSemesterCreditValidation entity, String validatedByName);

  SemesterCreditValidationResponse toRest(SemesterCreditValidation domain);

  @Named("toSemester")
  Semester toDomain(JSemester entity);

  SemesterResponse toRest(Semester semester);

  default AcademicYearResponse toRestAcademicYear(AcademicYear academicYear) {
    return new AcademicYearResponse()
        .id(academicYear.id())
        .label(academicYear.label())
        .startDate(academicYear.startDate())
        .endDate(academicYear.endDate());
  }
}
