package app.mata.gradup.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_graduation_eligibility")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JVGraduationEligibility {

  @Id
  @Column(name = "student_id")
  private UUID studentId;

  @Column(name = "cohort_id")
  private UUID cohortId;

  @Column(name = "track_id")
  private UUID trackId;

  @Column(name = "total_courses")
  private Long totalCourses;

  @Column(name = "passed_courses")
  private Long passedCourses;

  @Column(name = "is_eligible")
  private Boolean isEligible;

  @Column(name = "overall_average")
  private BigDecimal overallAverage;

  @Column(name = "min_grade")
  private BigDecimal minGrade;

  @Column(name = "max_grade")
  private BigDecimal maxGrade;
}
