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
@Table(name = "course")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JCourse {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "course_id")
  private UUID id;

  @Column(nullable = false, length = 30, unique = true)
  private String reference;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false)
  private int credits;

  @Column(name = "semester_number", nullable = false)
  private int semesterNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "track_id")
  private JTrack track;
}
