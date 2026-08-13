package app.mata.gradup.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_course_average")
@IdClass(JVCourseAverage.CourseAverageId.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JVCourseAverage {

  @Id
  @Column(name = "student_id")
  private UUID studentId;

  @Id
  @Column(name = "offering_id")
  private UUID offeringId;

  @Column(name = "course_id")
  private UUID courseId;

  @Column(name = "average")
  private BigDecimal average;

  @AllArgsConstructor
  @NoArgsConstructor
  @EqualsAndHashCode
  @Getter
  @Setter
  public static class CourseAverageId implements Serializable {
    private UUID studentId;
    private UUID offeringId;
  }
}
