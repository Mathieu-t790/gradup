package app.mata.gradup.repository.model;

import app.mata.gradup.model.TranscriptType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "transcript")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JTranscript {

  @Id
  @Column(name = "transcript_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id")
  private JStudent student;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TranscriptType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "semester_id")
  private JSemester semester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_year_id")
  private JAcademicYear academicYear;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "diploma_id")
  private JDiploma diploma;

  @Column(name = "overall_average", precision = 5, scale = 2)
  private BigDecimal overallAverage;

  @Column(name = "credits_earned")
  private Integer creditsEarned;

  @Generated(event = EventType.INSERT)
  @Column(name = "generated_at", insertable = false, updatable = false)
  private Instant generatedAt;

  @Column(name = "storage_key", length = 500)
  private String storageKey;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "recipient_email", length = 255)
  private String recipientEmail;
}
