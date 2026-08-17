package app.mata.gradup.repository;

import app.mata.gradup.model.DisputeStatus;
import app.mata.gradup.repository.model.JGradeDispute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeDisputeRepository extends JpaRepository<JGradeDispute, UUID> {

  Page<JGradeDispute> findByStatus(DisputeStatus status, Pageable pageable);

  Optional<JGradeDispute> findByGradeIdAndStatus(UUID gradeId, DisputeStatus status);

  List<JGradeDispute> findByStudentId(UUID studentId);
}
