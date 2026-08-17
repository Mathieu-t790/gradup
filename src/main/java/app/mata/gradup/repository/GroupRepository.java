package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<JGroup, UUID> {

  @EntityGraph(attributePaths = {"cohort", "track"})
  List<JGroup> findByCohortId(UUID cohortId);

  @EntityGraph(attributePaths = {"cohort", "track"})
  List<JGroup> findByTrackId(UUID trackId);

  @EntityGraph(attributePaths = {"cohort", "track"})
  List<JGroup> findAll();

  boolean existsByCohortIdAndReference(UUID cohortId, String reference);
}
