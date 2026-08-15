package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CourseCreateRequest;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
import app.mata.gradup.endpoint.rest.model.TrackSummary;
import app.mata.gradup.model.Course;
import app.mata.gradup.model.Track;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JTrack;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseMapper {

  Course toDomain(JCourse entity);

  CourseResponse toRest(Course course);

  @Mapping(source = "track", target = "track")
  @Mapping(target = "id", ignore = true)
  Course toDomain(CourseCreateRequest request, JTrack track);

  @Mapping(target = "id", ignore = true)
  JCourse toEntity(Course course);

  Track toTrack(JTrack entity);

  default TrackSummary toTrackSummary(Track track) {
    if (track == null) {
      return null;
    }
    return new TrackSummary()
        .id(track.id())
        .code(
            track.code() == null
                ? null
                : app.mata.gradup.endpoint.rest.model.TrackCode.valueOf(track.code().name()));
  }
}
