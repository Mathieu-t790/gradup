package app.mata.gradup.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exam")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JExam {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "exam_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "offering_id")
  private JCourseOffering offering;

  @Column(nullable = false, length = 100)
  private String label;

  @Column(name = "exam_date")
  private LocalDate examDate;

  @Column(name = "exam_time")
  private LocalTime examTime;

  @Column(name = "weight_numerator", nullable = false)
  private int weightNumerator;

  @Column(name = "weight_denominator", nullable = false)
  private int weightDenominator;
}
