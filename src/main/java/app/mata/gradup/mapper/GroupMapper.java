package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CohortSummary;
import app.mata.gradup.endpoint.rest.model.GroupResponse;
import app.mata.gradup.endpoint.rest.model.TrackSummary;
import app.mata.gradup.model.Cohort;
import app.mata.gradup.model.Group;
import app.mata.gradup.model.Track;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JTrack;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupMapper {

  Group toDomain(JGroup entity);

  GroupResponse toRest(Group group);

  default Cohort toCohort(JCohort entity) {
    return entity == null
        ? null
        : new Cohort(
            entity.getId(),
            entity.getLabel(),
            entity.getEntryYear(),
            entity.getExpectedGraduationYear());
  }

  default Track toTrack(JTrack entity) {
    return entity == null ? null : new Track(entity.getId(), entity.getCode(), entity.getLabel());
  }

  default CohortSummary toCohortSummary(Cohort cohort) {
    return cohort == null ? null : new CohortSummary().id(cohort.id()).label(cohort.label());
  }

  default TrackSummary toTrackSummary(Track track) {
    return track == null
        ? null
        : new TrackSummary().id(track.id()).code(toRestTrackCode(track.code()));
  }

  default app.mata.gradup.endpoint.rest.model.TrackCode toRestTrackCode(TrackCode code) {
    return code == null ? null : app.mata.gradup.endpoint.rest.model.TrackCode.valueOf(code.name());
  }
}
