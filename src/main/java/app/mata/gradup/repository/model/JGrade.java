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
@Table(name = "grade")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JGrade {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "grade_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id")
  private JStudent student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_id")
  private JExam exam;

  @Column(nullable = false, precision = 4, scale = 2)
  private BigDecimal score;

  @Generated(event = EventType.INSERT)
  @Column(name = "recorded_at", insertable = false, updatable = false)
  private Instant recordedAt;

  @Column(name = "recorded_by", nullable = false)
  private UUID recordedBy;
}
