package app.mata.gradup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.mata.gradup.endpoint.rest.model.ExamCreateRequest;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.endpoint.rest.model.ExamUpdateRequest;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.mapper.ExamMapper;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExamServiceTest {

  private final ExamRepository examRepository = mock(ExamRepository.class);
  private final CourseOfferingRepository courseOfferingRepository =
      mock(CourseOfferingRepository.class);
  private final ExamMapper examMapper = mock(ExamMapper.class);

  private final ExamService service =
      new ExamService(examRepository, courseOfferingRepository, examMapper);

  @Test
  void create_exam_rejects_weights_that_push_the_total_above_one() {
    JCourseOffering offering = offering();
    when(courseOfferingRepository.findById(offering.getId())).thenReturn(Optional.of(offering));
    when(examRepository.sumWeightsByOfferingId(offering.getId())).thenReturn(0.8);

    ExamCreateRequest request =
        new ExamCreateRequest().label("CC1").weightNumerator(3).weightDenominator(10);

    BusinessRuleException error =
        assertThrows(
            BusinessRuleException.class, () -> service.createExam(offering.getId(), request));

    assertTrue(error.getMessage().contains("Total exam weights"));
    verify(examRepository, never()).save(any());
  }

  @Test
  void create_exam_accepts_weights_that_reach_exactly_one_and_finalizes_grading() {
    JCourseOffering offering = offering();
    when(courseOfferingRepository.findById(offering.getId())).thenReturn(Optional.of(offering));
    when(examRepository.sumWeightsByOfferingId(offering.getId())).thenReturn(0.8);
    JExam saved = JExam.builder().id(UUID.randomUUID()).offering(offering).build();
    when(examRepository.save(any(JExam.class))).thenReturn(saved);
    when(examMapper.toRest(saved)).thenReturn(new ExamResponse());

    ExamCreateRequest request =
        new ExamCreateRequest().label("CC2").weightNumerator(2).weightDenominator(10);

    service.createExam(offering.getId(), request);

    assertTrue(offering.getGradingFinalized(), "total 1.0 must finalize grading");
    verify(courseOfferingRepository).save(offering);
  }

  @Test
  void update_exam_rejects_weights_that_exceed_one_against_the_other_exams() {
    JCourseOffering offering = offering();
    JExam exam =
        JExam.builder()
            .id(UUID.randomUUID())
            .offering(offering)
            .weightNumerator(2)
            .weightDenominator(10)
            .build();
    when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
    when(examRepository.sumWeightsByOfferingId(offering.getId())).thenReturn(1.1);

    ExamUpdateRequest request = new ExamUpdateRequest().weightNumerator(3).weightDenominator(10);

    assertThrows(BusinessRuleException.class, () -> service.updateExam(exam.getId(), request));
    verify(examRepository, never()).save(any());
  }

  @Test
  void update_exam_keeps_other_weights_when_replacing_its_own_weight() {
    JCourseOffering offering = offering();
    JExam exam =
        JExam.builder()
            .id(UUID.randomUUID())
            .offering(offering)
            .weightNumerator(4)
            .weightDenominator(10)
            .build();
    when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
    when(examRepository.sumWeightsByOfferingId(offering.getId())).thenReturn(1.0);
    when(examMapper.toRest(exam)).thenReturn(new ExamResponse());

    ExamUpdateRequest request = new ExamUpdateRequest().weightNumerator(3).weightDenominator(10);

    service.updateExam(exam.getId(), request);

    assertEquals(3, exam.getWeightNumerator());
    assertEquals(10, exam.getWeightDenominator());
    verify(examRepository).save(exam);
  }

  private static JCourseOffering offering() {
    return JCourseOffering.builder().id(UUID.randomUUID()).build();
  }
}
