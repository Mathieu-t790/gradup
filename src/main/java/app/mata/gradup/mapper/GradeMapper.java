package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.GradeResponse;
import app.mata.gradup.model.Grade;
import app.mata.gradup.repository.model.JGrade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GradeMapper {

  @Mapping(source = "entity.id", target = "id")
  @Mapping(source = "entity.student.id", target = "studentId")
  @Mapping(source = "studentName", target = "studentName")
  @Mapping(source = "entity.exam.id", target = "examId")
  @Mapping(source = "examLabel", target = "examLabel")
  @Mapping(source = "courseReference", target = "courseReference")
  @Mapping(source = "entity.score", target = "score")
  @Mapping(source = "entity.recordedAt", target = "recordedAt")
  @Mapping(source = "recordedByName", target = "recordedByName")
  Grade toDomain(
      JGrade entity,
      String studentName,
      String examLabel,
      String courseReference,
      String recordedByName);

  GradeResponse toRest(Grade domain);
}
