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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transcript_detail")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JTranscriptDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "detail_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transcript_id")
  private JTranscript transcript;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "offering_id")
  private JCourseOffering offering;

  @Column(name = "course_score", precision = 5, scale = 2)
  private BigDecimal courseScore;

  @Column(name = "credits_earned", nullable = false)
  private boolean creditsEarned;
}
