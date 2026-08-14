package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.TranscriptResponse;
import app.mata.gradup.repository.model.JTranscript;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TranscriptMapper {

  @Mapping(target = "id", source = "entity.id")
  @Mapping(target = "studentId", source = "entity.student.id")
  @Mapping(target = "type", source = "entity.type")
  @Mapping(target = "semesterId", source = "entity.semester.id")
  @Mapping(target = "academicYearId", source = "entity.academicYear.id")
  @Mapping(target = "diplomaId", source = "entity.diploma.id")
  @Mapping(target = "overallAverage", source = "entity.overallAverage")
  @Mapping(target = "creditsEarned", source = "entity.creditsEarned")
  @Mapping(target = "generatedAt", source = "entity.generatedAt")
  @Mapping(target = "sentAt", source = "entity.sentAt")
  @Mapping(target = "recipientEmail", source = "entity.recipientEmail")
  TranscriptResponse toRest(JTranscript entity, String downloadUrl);

  default app.mata.gradup.endpoint.rest.model.TranscriptType toRestType(
      app.mata.gradup.model.TranscriptType type) {
    return app.mata.gradup.endpoint.rest.model.TranscriptType.valueOf(type.name());
  }
}
