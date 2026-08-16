package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<JGroup, UUID> {

  List<JGroup> findByCohortId(UUID cohortId);

  List<JGroup> findByTrackId(UUID trackId);

  boolean existsByCohortIdAndReference(UUID cohortId, String reference);
}
