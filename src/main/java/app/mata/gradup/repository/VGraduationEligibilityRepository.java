package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JVGraduationEligibility;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VGraduationEligibilityRepository
    extends JpaRepository<JVGraduationEligibility, UUID> {

  Optional<JVGraduationEligibility> findByStudentId(UUID studentId);
}
