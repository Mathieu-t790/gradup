package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JSemesterCreditValidation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterCreditValidationRepository
    extends JpaRepository<JSemesterCreditValidation, UUID> {

  Optional<JSemesterCreditValidation> findBySemesterIdAndTrackId(UUID semesterId, UUID trackId);
}
