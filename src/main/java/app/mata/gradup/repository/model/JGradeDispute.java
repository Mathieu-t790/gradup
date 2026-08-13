package app.mata.gradup.repository.model;

import app.mata.gradup.model.DisputeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "grade_dispute")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JGradeDispute {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "dispute_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "grade_id")
  private JGrade grade;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id")
  private JStudent student;

  @Column(nullable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "status", nullable = false, length = 20)
  private DisputeStatus status = DisputeStatus.PENDING;

  @Generated(event = EventType.INSERT)
  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolved_by")
  private UUID resolvedBy;

  @Column(name = "resolution_note")
  private String resolutionNote;

  @Column(name = "resulting_history_id")
  private UUID resultingHistoryId;
}
