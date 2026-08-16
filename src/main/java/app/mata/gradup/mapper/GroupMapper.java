package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.GroupResponse;
import app.mata.gradup.model.Group;
import app.mata.gradup.repository.model.JGroup;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = ReferenceMapper.class)
public interface GroupMapper {

  Group toDomain(JGroup entity);

  GroupResponse toRest(Group group);
}
