package app.mata.gradup.service;

import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.CohortRepository;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GradeDisputeRepository;
import app.mata.gradup.repository.GradeRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.StudentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DashboardService {

  private final StudentRepository studentRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final GradeDisputeRepository gradeDisputeRepository;
  private final CohortRepository cohortRepository;
  private final AcademicYearRepository academicYearRepository;
  private final SemesterRepository semesterRepository;
  private final GradeRepository gradeRepository;

  public Counts counts() {
    return new Counts(
        studentRepository.count(),
        courseRepository.count(),
        examRepository.count(),
        gradeDisputeRepository.count(),
        cohortRepository.count(),
        academicYearRepository.count(),
        semesterRepository.count(),
        gradeRepository.count());
  }

  public record Counts(
      long students,
      long courses,
      long exams,
      long disputes,
      long cohorts,
      long academicYears,
      long semesters,
      long grades) {}
}
