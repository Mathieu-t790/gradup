package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.TrackCreateRequest;
import app.mata.gradup.endpoint.rest.model.TrackResponse;
import app.mata.gradup.model.Track;
import app.mata.gradup.repository.model.JTrack;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrackMapper {

  Track toDomain(JTrack jTrack);

  TrackResponse toRest(Track track);

  Track toDomain(TrackCreateRequest request);

  @Mapping(target = "id", ignore = true)
  JTrack toEntity(Track track);

  default app.mata.gradup.endpoint.rest.model.TrackCode toRestTrackCode(
      app.mata.gradup.model.TrackCode code) {
    return code == null ? null : app.mata.gradup.endpoint.rest.model.TrackCode.valueOf(code.name());
  }

  default app.mata.gradup.model.TrackCode toDomainTrackCode(
      app.mata.gradup.endpoint.rest.model.TrackCode code) {
    return code == null ? null : app.mata.gradup.model.TrackCode.valueOf(code.name());
  }
}
