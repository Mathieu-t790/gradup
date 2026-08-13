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
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "semester_credit_validation")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JSemesterCreditValidation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "validation_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "semester_id")
  private JSemester semester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "track_id")
  private JTrack track;

  @Column(name = "total_credits", nullable = false)
  private int totalCredits;

  @Generated(event = EventType.INSERT)
  @Column(name = "validated_at", insertable = false, updatable = false)
  private Instant validatedAt;

  @Column(name = "validated_by", nullable = false)
  private UUID validatedBy;
}
