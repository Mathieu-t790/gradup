package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CohortCreateRequest;
import app.mata.gradup.endpoint.rest.model.CohortResponse;
import app.mata.gradup.model.Cohort;
import app.mata.gradup.repository.model.JCohort;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CohortMapper {

  Cohort toDomain(JCohort jCohort);

  CohortResponse toRest(Cohort cohort);

  Cohort toDomain(CohortCreateRequest request);

  @Mapping(target = "id", ignore = true)
  JCohort toEntity(Cohort cohort);
}
