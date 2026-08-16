package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.SemesterCreditValidationResponse;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.SemesterMapper;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.SemesterCreditValidationRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JSemesterCreditValidation;
import app.mata.gradup.service.utils.Users;
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
  private final SemesterMapper semesterMapper;

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
        courseOfferingRepository.sumCreditsBySemesterIdAndTrackId(semesterId, trackId);
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
