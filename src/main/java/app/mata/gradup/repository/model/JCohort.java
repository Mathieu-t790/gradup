package app.mata.gradup.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cohort")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JCohort {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "cohort_id")
  private UUID id;

  @Column(nullable = false, length = 100)
  private String label;

  @Column(name = "entry_year", nullable = false)
  private int entryYear;

  @Column(name = "expected_graduation_year", nullable = false)
  private int expectedGraduationYear;
}
