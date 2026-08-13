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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "groups")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "group_id")
  private UUID id;

  @Column(nullable = false, length = 20)
  private String reference;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cohort_id")
  private JCohort cohort;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "track_id")
  private JTrack track;
}
