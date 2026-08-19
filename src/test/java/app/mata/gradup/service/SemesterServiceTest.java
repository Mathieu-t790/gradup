package app.mata.gradup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.mata.gradup.endpoint.rest.model.SemesterCreditValidationResponse;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.mapper.SemesterMapper;
import app.mata.gradup.model.Semester;
import app.mata.gradup.model.SemesterCreditValidation;
import app.mata.gradup.model.Track;
import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.SemesterCreditValidationRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.UserRepository;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JSemesterCreditValidation;
import app.mata.gradup.repository.model.JTrack;
import app.mata.gradup.repository.model.JUser;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SemesterServiceTest {

  private final SemesterRepository semesterRepository = mock(SemesterRepository.class);
  private final TrackRepository trackRepository = mock(TrackRepository.class);
  private final SemesterCreditValidationRepository validationRepository =
      mock(SemesterCreditValidationRepository.class);
  private final CourseOfferingRepository courseOfferingRepository =
      mock(CourseOfferingRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final AcademicYearRepository academicYearRepository = mock(AcademicYearRepository.class);
  private final SemesterMapper semesterMapper = mock(SemesterMapper.class);

  private final SemesterService service =
      new SemesterService(
          semesterRepository,
          trackRepository,
          validationRepository,
          courseOfferingRepository,
          userRepository,
          academicYearRepository,
          semesterMapper);

  @Test
  void finalize_rejects_a_semester_that_does_not_sum_to_exactly_30_credits() {
    UUID semesterId = UUID.randomUUID();
    UUID trackId = UUID.randomUUID();
    when(semesterRepository.findById(semesterId))
        .thenReturn(Optional.of(JSemester.builder().id(semesterId).build()));
    when(trackRepository.findById(trackId))
        .thenReturn(Optional.of(JTrack.builder().id(trackId).build()));
    when(validationRepository.findBySemesterIdAndTrackId(semesterId, trackId))
        .thenReturn(Optional.empty());
    when(courseOfferingRepository.sumCreditsBySemesterIdAndTrackId(semesterId, trackId, null))
        .thenReturn(29);

    BusinessRuleException error =
        assertThrows(
            BusinessRuleException.class,
            () -> service.finalizeSemesterCredits(semesterId, trackId, UUID.randomUUID()));

    assertTrue(error.getMessage().contains("must be exactly 30"));
    verify(validationRepository, never()).saveAndFlush(any());
  }

  @Test
  void finalize_rejects_a_semester_that_is_already_finalized_for_the_track() {
    UUID semesterId = UUID.randomUUID();
    UUID trackId = UUID.randomUUID();
    when(semesterRepository.findById(semesterId))
        .thenReturn(Optional.of(JSemester.builder().id(semesterId).build()));
    when(trackRepository.findById(trackId))
        .thenReturn(Optional.of(JTrack.builder().id(trackId).build()));
    when(validationRepository.findBySemesterIdAndTrackId(semesterId, trackId))
        .thenReturn(
            Optional.of(
                JSemesterCreditValidation.builder()
                    .id(UUID.randomUUID())
                    .semester(JSemester.builder().id(semesterId).build())
                    .track(JTrack.builder().id(trackId).build())
                    .totalCredits(30)
                    .build()));

    assertThrows(
        ConflictException.class,
        () -> service.finalizeSemesterCredits(semesterId, trackId, UUID.randomUUID()));
  }

  @Test
  void finalize_persists_the_validation_with_the_exact_30_credits() {
    UUID semesterId = UUID.randomUUID();
    UUID trackId = UUID.randomUUID();
    UUID adminUserId = UUID.randomUUID();
    when(semesterRepository.findById(semesterId))
        .thenReturn(Optional.of(JSemester.builder().id(semesterId).build()));
    when(trackRepository.findById(trackId))
        .thenReturn(Optional.of(JTrack.builder().id(trackId).build()));
    when(validationRepository.findBySemesterIdAndTrackId(semesterId, trackId))
        .thenReturn(Optional.empty());
    when(courseOfferingRepository.sumCreditsBySemesterIdAndTrackId(semesterId, trackId, null))
        .thenReturn(30);
    when(userRepository.findById(adminUserId))
        .thenReturn(
            Optional.of(
                JUser.builder()
                    .id(adminUserId)
                    .lastName("Mathieu")
                    .firstName("Tafita")
                    .email("admin@cu.te")
                    .build()));
    JSemesterCreditValidation saved =
        JSemesterCreditValidation.builder()
            .id(UUID.randomUUID())
            .semester(JSemester.builder().id(semesterId).build())
            .track(JTrack.builder().id(trackId).build())
            .totalCredits(30)
            .validatedBy(adminUserId)
            .build();
    when(validationRepository.saveAndFlush(any(JSemesterCreditValidation.class)))
        .thenReturn(saved);
    SemesterCreditValidation domain =
        new SemesterCreditValidation(
            saved.getId(),
            new Semester(null, 1, null, null, null),
            new Track(trackId, null, null),
            30,
            Instant.now(),
            "Tafita Mathieu");
    when(semesterMapper.toDomain(any(JSemesterCreditValidation.class), eq("Tafita Mathieu")))
        .thenReturn(domain);
    when(semesterMapper.toRest(domain)).thenReturn(new SemesterCreditValidationResponse());

    service.finalizeSemesterCredits(semesterId, trackId, adminUserId);

    ArgumentCaptor<JSemesterCreditValidation> captor =
        ArgumentCaptor.forClass(JSemesterCreditValidation.class);
    verify(validationRepository).saveAndFlush(captor.capture());
    assertEquals(30, captor.getValue().getTotalCredits());
    assertEquals(adminUserId, captor.getValue().getValidatedBy());
  }
}