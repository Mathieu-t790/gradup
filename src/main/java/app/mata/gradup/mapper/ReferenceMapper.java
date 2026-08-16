package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CohortSummary;
import app.mata.gradup.endpoint.rest.model.TrackSummary;
import app.mata.gradup.model.Cohort;
import app.mata.gradup.model.Track;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JTrack;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReferenceMapper {

  Cohort toCohort(JCohort entity);

  Track toTrack(JTrack entity);

  CohortSummary toCohortSummary(Cohort cohort);

  TrackSummary toTrackSummary(Track track);

  default app.mata.gradup.endpoint.rest.model.TrackCode toRestTrackCode(TrackCode code) {
    return code == null ? null : app.mata.gradup.endpoint.rest.model.TrackCode.valueOf(code.name());
  }

  default TrackCode toDomainTrackCode(app.mata.gradup.endpoint.rest.model.TrackCode code) {
    return code == null ? null : TrackCode.valueOf(code.name());
  }
}
