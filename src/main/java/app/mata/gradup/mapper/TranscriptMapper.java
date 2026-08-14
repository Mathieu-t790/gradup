package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.TranscriptResponse;
import app.mata.gradup.model.Transcript;
import app.mata.gradup.repository.model.JTranscript;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TranscriptMapper {

  @Mapping(source = "entity.student.id", target = "studentId")
  @Mapping(source = "entity.semester.id", target = "semesterId")
  @Mapping(source = "entity.academicYear.id", target = "academicYearId")
  @Mapping(source = "entity.diploma.id", target = "diplomaId")
  @Mapping(source = "downloadUrl", target = "downloadUrl")
  Transcript toDomain(JTranscript entity, String downloadUrl);

  TranscriptResponse toRest(Transcript domain);

  default app.mata.gradup.endpoint.rest.model.TranscriptType toRestType(
      app.mata.gradup.model.TranscriptType type) {
    return type == null
        ? null
        : app.mata.gradup.endpoint.rest.model.TranscriptType.valueOf(type.name());
  }
}
