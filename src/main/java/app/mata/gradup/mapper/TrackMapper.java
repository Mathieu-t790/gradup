package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.TrackCreateRequest;
import app.mata.gradup.endpoint.rest.model.TrackResponse;
import app.mata.gradup.model.Track;
import app.mata.gradup.repository.model.JTrack;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = ReferenceMapper.class)
public interface TrackMapper {

  Track toDomain(JTrack jTrack);

  TrackResponse toRest(Track track);

  Track toDomain(TrackCreateRequest request);

  @Mapping(target = "id", ignore = true)
  JTrack toEntity(Track track);
}
