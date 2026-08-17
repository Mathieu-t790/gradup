package app.mata.gradup.mapper;

import app.mata.gradup.endpoint.rest.model.CohortSummary;
import app.mata.gradup.endpoint.rest.model.SemesterSummary;
import app.mata.gradup.endpoint.rest.model.TrackSummary;
import app.mata.gradup.model.AcademicYear;
import app.mata.gradup.model.Cohort;
import app.mata.gradup.model.Semester;
import app.mata.gradup.model.Track;
import app.mata.gradup.model.TrackCode;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCohort;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.service.utils.TrackCodes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReferenceMapper {

  Cohort toCohort(JCohort entity);

  Track toTrack(JTrack entity);

  AcademicYear toAcademicYear(JAcademicYear entity);

  Semester toSemester(JSemester entity);

  CohortSummary toCohortSummary(Cohort cohort);

  TrackSummary toTrackSummary(Track track);

  @Mapping(target = "academicYearLabel", source = "academicYear.label")
  SemesterSummary toSemesterSummary(Semester semester);

  default app.mata.gradup.endpoint.rest.model.TrackCode toRestTrackCode(TrackCode code) {
    return TrackCodes.toRest(code);
  }

  default TrackCode toDomainTrackCode(app.mata.gradup.endpoint.rest.model.TrackCode code) {
    return TrackCodes.toDomain(code);
  }
}
