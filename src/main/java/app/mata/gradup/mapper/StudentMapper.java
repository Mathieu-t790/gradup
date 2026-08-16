package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CohortSummary;
import app.mata.gradup.endpoint.rest.model.GroupSummary;
import app.mata.gradup.endpoint.rest.model.StudentGroupHistoryResponse;
import app.mata.gradup.endpoint.rest.model.StudentResponse;
import app.mata.gradup.endpoint.rest.model.StudentSummaryResponse;
import app.mata.gradup.endpoint.rest.model.StudentTrackHistoryResponse;
import app.mata.gradup.endpoint.rest.model.TrackSummary;
import app.mata.gradup.model.Cohort;
import app.mata.gradup.model.Course;
import app.mata.gradup.model.Group;
import app.mata.gradup.model.Student;
import app.mata.gradup.model.StudentGroupHistory;
import app.mata.gradup.model.StudentTrackHistory;
import app.mata.gradup.model.Track;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.model.User;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JGroup;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JStudentGroupHistory;
import app.mata.gradup.repository.model.JStudentTrackHistory;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.repository.model.JUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StudentMapper {

  @Mapping(source = "entity.user", target = "user")
  @Mapping(source = "entity.cohort", target = "cohort")
  @Mapping(source = "currentGroup", target = "currentGroup")
  @Mapping(source = "currentTrack", target = "currentTrack")
  Student toDomain(JStudent entity, Group currentGroup, Track currentTrack);

  @Mapping(source = "user.id", target = "id")
  @Mapping(source = "user.lastName", target = "lastName")
  @Mapping(source = "user.firstName", target = "firstName")
  @Mapping(source = "user.email", target = "email")
  @Mapping(source = "user.reference", target = "reference")
  @Mapping(source = "user.isActive", target = "isActive")
  StudentResponse toRest(Student domain);

  @Mapping(source = "entity.user.id", target = "id")
  @Mapping(source = "entity.user.lastName", target = "lastName")
  @Mapping(source = "entity.user.firstName", target = "firstName")
  @Mapping(source = "entity.user.reference", target = "reference")
  @Mapping(source = "entity.cohort.label", target = "cohortLabel")
  @Mapping(target = "currentGroupReference", source = "currentGroup")
  @Mapping(target = "currentTrackCode", source = "currentTrack")
  StudentSummaryResponse toRestSummary(JStudent entity, Group currentGroup, Track currentTrack);

  default app.mata.gradup.endpoint.rest.model.TrackCode toRestTrackCode(TrackCode code) {
    return code == null ? null : app.mata.gradup.endpoint.rest.model.TrackCode.valueOf(code.name());
  }

  default JsonNullable<String> toCurrentGroupReference(Group group) {
    return group == null ? JsonNullable.undefined() : JsonNullable.of(group.reference());
  }

  default app.mata.gradup.endpoint.rest.model.TrackCode toCurrentTrackCode(Track track) {
    return track == null ? null : toRestTrackCode(track.code());
  }

  default <T> T nullableOrNull(JsonNullable<T> value) {
    return value == null ? null : value.orElse(null);
  }

  StudentGroupHistory toDomain(JStudentGroupHistory entity);

  StudentGroupHistoryResponse toRest(StudentGroupHistory domain);

  StudentTrackHistory toDomain(JStudentTrackHistory entity);

  StudentTrackHistoryResponse toRest(StudentTrackHistory domain);

  User toUser(JUser entity);

  Cohort toCohort(JCohort entity);

  Group toGroup(JGroup entity);

  Track toTrack(JTrack entity);

  Course toCourse(JCourse entity);

  CohortSummary toCohortSummary(Cohort cohort);

  GroupSummary toGroupSummary(Group group);

  TrackSummary toTrackSummary(Track track);
}
