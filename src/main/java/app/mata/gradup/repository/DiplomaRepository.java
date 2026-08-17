package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JDiploma;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiplomaRepository extends JpaRepository<JDiploma, UUID> {

  @EntityGraph(attributePaths = {"student", "student.user", "cohort", "track"})
  Page<JDiploma> findByCohortIdAndTrackId(UUID cohortId, UUID trackId, Pageable pageable);

  @EntityGraph(attributePaths = {"student", "student.user", "cohort", "track"})
  Page<JDiploma> findByCohortId(UUID cohortId, Pageable pageable);

  Optional<JDiploma> findByStudentId(UUID studentId);

  @EntityGraph(attributePaths = {"student", "student.user", "cohort", "track"})
  List<JDiploma> findByCohortIdAndTrackId(UUID cohortId, UUID trackId);

  @EntityGraph(attributePaths = {"student", "student.user", "cohort", "track"})
  List<JDiploma> findByCohortId(UUID cohortId);
}
