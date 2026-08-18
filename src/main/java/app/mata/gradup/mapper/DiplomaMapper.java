package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CohortSummary;
import app.mata.gradup.endpoint.rest.model.DiplomaResponse;
import app.mata.gradup.endpoint.rest.model.StudentSummaryResponse;
import app.mata.gradup.endpoint.rest.model.TrackSummary;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.model.JDiploma;
import app.mata.gradup.repository.model.JUser;
import app.mata.gradup.service.utils.TrackCodes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DiplomaMapper {

  @Mapping(target = "student", expression = "java(toStudentSummary(entity))")
  @Mapping(target = "cohort", expression = "java(toCohortSummary(entity))")
  @Mapping(target = "track", expression = "java(toTrackSummary(entity))")
  @Mapping(target = "overallAverage", expression = "java(toDouble(entity.getOverallAverage()))")
  @Mapping(target = "graduationDate", source = "entity.graduationDate")
  @Mapping(target = "listGeneratedAt", source = "entity.listGeneratedAt")
  DiplomaResponse toRest(JDiploma entity);

  default StudentSummaryResponse toStudentSummary(JDiploma entity) {
    JUser user = entity.getStudent().getUser();
    return new StudentSummaryResponse()
        .id(user.getId())
        .lastName(user.getLastName())
        .firstName(user.getFirstName())
        .reference(user.getReference())
        .cohortLabel(entity.getCohort().getLabel())
        .currentTrackCode(toRestTrackCode(trackCode(entity)));
  }

  default CohortSummary toCohortSummary(JDiploma entity) {
    return new CohortSummary().id(entity.getCohort().getId()).label(entity.getCohort().getLabel());
  }

  default TrackSummary toTrackSummary(JDiploma entity) {
    if (entity.getTrack() == null) {
      return null;
    }
    return new TrackSummary()
        .id(entity.getTrack().getId())
        .code(toRestTrackCode(entity.getTrack().getCode()));
  }

  default TrackCode trackCode(JDiploma entity) {
    return entity.getTrack() == null ? null : entity.getTrack().getCode();
  }

  default Double toDouble(BigDecimal value) {
    return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  default app.mata.gradup.endpoint.rest.model.TrackCode toRestTrackCode(TrackCode code) {
    return TrackCodes.toRest(code);
  }
}
