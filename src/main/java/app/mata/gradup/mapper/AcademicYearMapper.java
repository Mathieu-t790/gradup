package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.AcademicYearResponse;
import app.mata.gradup.model.AcademicYear;
import app.mata.gradup.repository.model.JAcademicYear;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AcademicYearMapper {

  AcademicYear toDomain(JAcademicYear entity);

  AcademicYearResponse toRest(AcademicYear academicYear);
}
