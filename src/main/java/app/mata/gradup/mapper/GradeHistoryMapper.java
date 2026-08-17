package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.GradeHistoryEntryResponse;
import app.mata.gradup.model.GradeHistory;
import app.mata.gradup.repository.model.JGradeHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GradeHistoryMapper {

  @Mapping(source = "entity.id", target = "id")
  @Mapping(source = "entity.oldScore", target = "oldScore")
  @Mapping(source = "entity.newScore", target = "newScore")
  @Mapping(source = "modifiedByName", target = "modifiedByName")
  @Mapping(source = "entity.modifiedAt", target = "modifiedAt")
  @Mapping(source = "entity.reason", target = "reason")
  GradeHistory toDomain(JGradeHistory entity, String modifiedByName);

  GradeHistoryEntryResponse toRest(GradeHistory domain);
}
