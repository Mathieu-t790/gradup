package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.TeacherResponse;
import app.mata.gradup.model.Teacher;
import app.mata.gradup.model.User;
import app.mata.gradup.repository.model.JTeacher;
import app.mata.gradup.repository.model.JUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TeacherMapper {

  @Mapping(source = "entity.user", target = "user")
  Teacher toDomain(JTeacher entity);

  @Mapping(source = "user.id", target = "id")
  @Mapping(source = "user.lastName", target = "lastName")
  @Mapping(source = "user.firstName", target = "firstName")
  @Mapping(source = "user.email", target = "email")
  @Mapping(source = "user.reference", target = "reference")
  TeacherResponse toRest(Teacher domain);

  default User toUser(JUser entity) {
    return entity == null
        ? null
        : new User(
            entity.getId(),
            entity.getReference(),
            entity.getLastName(),
            entity.getFirstName(),
            entity.getEmail(),
            entity.getPhone(),
            entity.getRole(),
            entity.getIsActive());
  }

  default <T> T nullableOrNull(JsonNullable<T> value) {
    return value == null ? null : value.orElse(null);
  }
}
