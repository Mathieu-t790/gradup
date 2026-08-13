package app.mata.gradup.repository.model;

import app.mata.gradup.model.TrackCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "track")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JTrack {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "track_id")
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, unique = true)
  private TrackCode code;

  @Column(nullable = false, length = 100)
  private String label;
}
