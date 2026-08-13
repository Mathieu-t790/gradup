package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JCohort;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CohortRepository extends JpaRepository<JCohort, UUID> {}
