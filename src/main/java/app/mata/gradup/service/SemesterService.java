package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.SemesterCreateRequest;
import app.mata.gradup.endpoint.rest.model.SemesterCreditValidationResponse;
import app.mata.gradup.endpoint.rest.model.SemesterResponse;
import app.mata.gradup.exception.BadRequestException;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.SemesterMapper;
import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.SemesterCreditValidationRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JSemesterCreditValidation;
import app.mata.gradup.service.utils.Users;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class SemesterService {

  private static final int REQUIRED_CREDITS = 30;

  private final SemesterRepository semesterRepository;
  private final TrackRepository trackRepository;
  private final SemesterCreditValidationRepository semesterCreditValidationRepository;
  private final CourseOfferingRepository courseOfferingRepository;
  private final UserRepository userRepository;
  private final AcademicYearRepository academicYearRepository;
  private final SemesterMapper semesterMapper;

  @Transactional(readOnly = true)
  public List<SemesterResponse> listSemesters(UUID academicYearId) {
    var semesters =
        academicYearId == null
            ? semesterRepository.findAll()
            : semesterRepository.findByAcademicYearId(academicYearId);
    return semesters.stream().map(semesterMapper::toDomain).map(semesterMapper::toRest).toList();
  }

  @Transactional
  public SemesterResponse createSemester(SemesterCreateRequest request) {
    var number = request.getNumber();
    if (number == null) {
      throw new BadRequestException("Semester number must be specified");
    }
    var academicYearId = request.getAcademicYearId();
    if (academicYearId == null) {
      throw new BadRequestException("Academic year must be specified");
    }
    if (!request.getStartDate().isBefore(request.getEndDate())) {
      throw new BadRequestException("Semester start date must be before end date");
    }
    var academicYear =
        academicYearRepository
            .findById(academicYearId)
            .orElseThrow(() -> new NotFoundException("Academic year not found: " + academicYearId));
    if (semesterRepository.existsByNumberAndAcademicYearId(number, academicYearId)) {
      throw new ConflictException(
          "A semester with number " + number + " already exists in this academic year");
    }
    var saved =
        semesterRepository.save(
            JSemester.builder()
                .number(number)
                .academicYear(academicYear)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build());
    return semesterMapper.toRest(semesterMapper.toDomain(saved));
  }

  @Transactional
  public SemesterCreditValidationResponse finalizeSemesterCredits(
      UUID semesterId, UUID trackId, UUID adminUserId) {
    var semester =
        semesterRepository
            .findById(semesterId)
            .orElseThrow(() -> new NotFoundException("Semester not found: " + semesterId));
    var track =
        trackRepository
            .findById(trackId)
            .orElseThrow(() -> new NotFoundException("Track not found: " + trackId));
    if (semesterCreditValidationRepository
        .findBySemesterIdAndTrackId(semesterId, trackId)
        .isPresent()) {
      throw new ConflictException(
          "Semester " + semesterId + " is already finalized for track " + trackId);
    }

    var totalCredits =
        courseOfferingRepository.sumCreditsBySemesterIdAndTrackId(semesterId, trackId, null);
    if (totalCredits != REQUIRED_CREDITS) {
      throw new BusinessRuleException(
          "Semester credits must be exactly 30 but was " + totalCredits);
    }

    var admin =
        userRepository
            .findById(adminUserId)
            .orElseThrow(() -> new NotFoundException("User not found: " + adminUserId));
    var validation =
        semesterCreditValidationRepository.saveAndFlush(
            JSemesterCreditValidation.builder()
                .semester(semester)
                .track(track)
                .totalCredits(totalCredits)
                .validatedBy(adminUserId)
                .build());
    return semesterMapper.toRest(semesterMapper.toDomain(validation, Users.fullName(admin)));
  }
}
