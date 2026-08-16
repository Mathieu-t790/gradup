package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JDiploma;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiplomaRepository extends JpaRepository<JDiploma, UUID> {

  Page<JDiploma> findByCohortIdAndTrackId(UUID cohortId, UUID trackId, Pageable pageable);

  Page<JDiploma> findByCohortId(UUID cohortId, Pageable pageable);

  Optional<JDiploma> findByStudentId(UUID studentId);

  List<JDiploma> findByCohortIdAndTrackId(UUID cohortId, UUID trackId);

  List<JDiploma> findByCohortId(UUID cohortId);
}
