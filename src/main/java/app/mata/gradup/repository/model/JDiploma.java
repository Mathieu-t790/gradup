package app.mata.gradup.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "diploma")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JDiploma {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "diploma_id")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", unique = true)
  private JStudent student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cohort_id")
  private JCohort cohort;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "track_id")
  private JTrack track;

  @Column(name = "overall_average", nullable = false, precision = 5, scale = 2)
  private BigDecimal overallAverage;

  @Column(nullable = false)
  private int rank;

  @Generated(event = EventType.INSERT)
  @Column(name = "graduation_date", insertable = false, updatable = false)
  private LocalDate graduationDate;

  @Generated(event = EventType.INSERT)
  @Column(name = "list_generated_at", insertable = false, updatable = false)
  private Instant listGeneratedAt;
}
