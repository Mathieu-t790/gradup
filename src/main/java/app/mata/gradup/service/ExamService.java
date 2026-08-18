package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.ExamCreateRequest;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.endpoint.rest.model.ExamUpdateRequest;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.ExamMapper;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JExam;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ExamService {

  private static final double WEIGHT_EPSILON = 1e-9;
  private static final double MAX_WEIGHT = 1.0;

  private final ExamRepository examRepository;
  private final CourseOfferingRepository courseOfferingRepository;
  private final ExamMapper examMapper;

  @Transactional(readOnly = true)
  public ExamResponse getExam(UUID examId) {
    var exam =
        examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Exam not found"));
    return examMapper.toRest(exam);
  }

  @Transactional
  public ExamResponse createExam(UUID offeringId, ExamCreateRequest request) {
    var offering =
        courseOfferingRepository
            .findById(offeringId)
            .orElseThrow(() -> new NotFoundException("Course offering not found"));
    double weight = weightOf(request.getWeightNumerator(), request.getWeightDenominator());
    double total = examRepository.sumWeightsByOfferingId(offeringId) + weight;
    if (total > MAX_WEIGHT + WEIGHT_EPSILON) {
      throw new BusinessRuleException("Total exam weights for this offering would exceed 1.0");
    }
    var exam =
        examRepository.save(
            JExam.builder()
                .offering(offering)
                .label(request.getLabel())
                .examDate(request.getExamDate())
                .examTime(toLocalTime(request.getExamTime()))
                .weightNumerator(request.getWeightNumerator())
                .weightDenominator(request.getWeightDenominator())
                .build());
    updateGradingFinalized(offering, total);
    return examMapper.toRest(exam);
  }

  @Transactional
  public ExamResponse updateExam(UUID examId, ExamUpdateRequest request) {
    var exam =
        examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Exam not found"));
    var offering = exam.getOffering();
    if (request.getLabel() != null) {
      exam.setLabel(request.getLabel());
    }
    if (request.getExamDate() != null) {
      exam.setExamDate(request.getExamDate());
    }
    if (request.getExamTime() != null) {
      exam.setExamTime(toLocalTime(request.getExamTime()));
    }
    if (request.getWeightNumerator() != null && request.getWeightDenominator() != null) {
      double newWeight = weightOf(request.getWeightNumerator(), request.getWeightDenominator());
      double otherWeights =
          examRepository.sumWeightsByOfferingId(offering.getId()) - weightOf(exam);
      double total = otherWeights + newWeight;
      if (total > MAX_WEIGHT + WEIGHT_EPSILON) {
        throw new BusinessRuleException("Total exam weights for this offering would exceed 1.0");
      }
      exam.setWeightNumerator(request.getWeightNumerator());
      exam.setWeightDenominator(request.getWeightDenominator());
      updateGradingFinalized(offering, total);
    }
    return examMapper.toRest(examRepository.save(exam));
  }

  private void updateGradingFinalized(JCourseOffering offering, double total) {
    if (Math.abs(total - MAX_WEIGHT) <= WEIGHT_EPSILON) {
      offering.setGradingFinalized(true);
      courseOfferingRepository.save(offering);
    }
  }

  private static double weightOf(JExam exam) {
    return (double) exam.getWeightNumerator() / exam.getWeightDenominator();
  }

  private static double weightOf(int numerator, int denominator) {
    return (double) numerator / denominator;
  }

  private static LocalTime toLocalTime(String time) {
    return time == null ? null : LocalTime.parse(time);
  }
}
