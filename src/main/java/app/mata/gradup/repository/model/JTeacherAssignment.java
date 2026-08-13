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
@Table(name = "teacher_assignment")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JTeacherAssignment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "offering_id")
  private JCourseOffering offering;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "teacher_id")
  private JTeacher teacher;
}
