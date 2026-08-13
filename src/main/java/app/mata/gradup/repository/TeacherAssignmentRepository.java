package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JTeacherAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<JTeacherAssignment, UUID> {

  List<JTeacherAssignment> findByTeacherId(UUID teacherId);

  @Transactional
  void deleteByOfferingIdAndTeacherId(UUID offeringId, UUID teacherId);
}
