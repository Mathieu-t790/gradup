package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JTeacherAssignment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<JTeacherAssignment, UUID> {

  List<JTeacherAssignment> findByTeacherId(UUID teacherId);

  @EntityGraph(attributePaths = {"teacher", "teacher.user"})
  List<JTeacherAssignment> findByOfferingId(UUID offeringId);

  @Query(
      "select a from JTeacherAssignment a "
          + "join fetch a.teacher t "
          + "join fetch t.user "
          + "where a.offering.id in :offeringIds")
  List<JTeacherAssignment> findByOfferingIdIn(@Param("offeringIds") Collection<UUID> offeringIds);

  boolean existsByTeacherIdAndOfferingId(UUID teacherId, UUID offeringId);

  @Transactional
  void deleteByOfferingIdAndTeacherId(UUID offeringId, UUID teacherId);
}
