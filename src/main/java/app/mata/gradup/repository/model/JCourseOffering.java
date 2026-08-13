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
@Table(name = "course_offering")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JCourseOffering {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "offering_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id")
  private JCourse course;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id")
  private JGroup group;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "semester_id")
  private JSemester semester;

  @Builder.Default
  @Column(name = "grading_finalized", nullable = false)
  private Boolean gradingFinalized = false;
}
