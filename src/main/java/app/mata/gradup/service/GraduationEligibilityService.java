package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.GraduationEligibilityResponse;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.GraduationEligibilityMapper;
import app.mata.gradup.mapper.StudentMapper;
import app.mata.gradup.model.FailingCourse;
import app.mata.gradup.model.GraduationEligibility;
import app.mata.gradup.model.Track;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.StudentTrackHistoryRepository;
import app.mata.gradup.repository.VCourseAverageRepository;
import app.mata.gradup.repository.VGraduationEligibilityRepository;
import app.mata.gradup.service.utils.Pages;
import app.mata.gradup.service.utils.Students;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GraduationEligibilityService {

  private static final BigDecimal PASSING_THRESHOLD = new BigDecimal("10");

  private final StudentRepository studentRepository;
  private final StudentTrackHistoryRepository studentTrackHistoryRepository;
  private final VGraduationEligibilityRepository eligibilityRepository;
  private final VCourseAverageRepository courseAverageRepository;
  private final CourseRepository courseRepository;
  private final GraduationEligibilityMapper eligibilityMapper;
  private final StudentMapper studentMapper;

  @Transactional(readOnly = true)
  public GraduationEligibilityResponse getGraduationEligibility(UUID studentId) {
    Students.requireStudent(studentRepository, studentId);
    var track = currentTrack(studentId);
    var eligibility = eligibilityRepository.findByStudentId(studentId).orElse(null);
    var overallAverage = eligibility == null ? null : eligibility.getOverallAverage();
    var isEligible = eligibility != null && Boolean.TRUE.equals(eligibility.getIsEligible());
    var failingCourses = failingCourses(studentId);
    return eligibilityMapper.toRest(
        new GraduationEligibility(studentId, track, isEligible, overallAverage, failingCourses));
  }

  private List<FailingCourse> failingCourses(UUID studentId) {
    return Pages.allPages(
            pageable ->
                courseAverageRepository.findByStudentIdAndAverageLessThan(
                    studentId, PASSING_THRESHOLD, pageable),
            Pages.DEFAULT_PAGE_SIZE)
        .stream()
        .map(
            average -> {
              var course =
                  courseRepository
                      .findById(average.getCourseId())
                      .orElseThrow(
                          () ->
                              new NotFoundException("Course not found: " + average.getCourseId()));
              return new FailingCourse(studentMapper.toCourse(course), average.getAverage());
            })
        .toList();
  }

  private Track currentTrack(UUID studentId) {
    return studentTrackHistoryRepository
        .findFirstByStudentIdAndEndDateIsNull(studentId)
        .map(history -> studentMapper.toTrack(history.getTrack()))
        .orElse(null);
  }
}
