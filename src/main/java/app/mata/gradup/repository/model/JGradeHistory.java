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
import java.math.BigDecimal;
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
@Table(name = "grade_history")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JGradeHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "history_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "grade_id")
  private JGrade grade;

  @Column(name = "old_score", precision = 4, scale = 2)
  private BigDecimal oldScore;

  @Column(name = "new_score", nullable = false, precision = 4, scale = 2)
  private BigDecimal newScore;

  @Column(name = "modified_by", nullable = false)
  private UUID modifiedBy;

  @Generated(event = EventType.INSERT)
  @Column(name = "modified_at", insertable = false, updatable = false)
  private Instant modifiedAt;

  @Column(length = 255)
  private String reason;
}
