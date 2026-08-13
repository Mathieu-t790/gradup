package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JStudent;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<JStudent, UUID> {

  Page<JStudent> findByCohortId(UUID cohortId, Pageable pageable);
}
