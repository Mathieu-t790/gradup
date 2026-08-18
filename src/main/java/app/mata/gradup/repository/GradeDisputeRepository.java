package app.mata.gradup.repository;

import app.mata.gradup.model.DisputeStatus;
import app.mata.gradup.repository.model.JGradeDispute;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeDisputeRepository extends JpaRepository<JGradeDispute, UUID> {

  @EntityGraph(
      attributePaths = {
        "grade",
        "grade.exam",
        "grade.exam.offering",
        "grade.exam.offering.course",
        "student",
        "student.user"
      })
  Page<JGradeDispute> findByStatus(DisputeStatus status, Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "grade",
        "grade.exam",
        "grade.exam.offering",
        "grade.exam.offering.course",
        "student",
        "student.user"
      })
  @Query(
      "SELECT d FROM JGradeDispute d WHERE d.status = :status AND d.grade.exam.offering.id"
          + " IN :offeringIds")
  Page<JGradeDispute> findByStatusAndOfferingIds(
      @Param("status") DisputeStatus status,
      @Param("offeringIds") Collection<UUID> offeringIds,
      Pageable pageable);

  Optional<JGradeDispute> findByGradeIdAndStatus(UUID gradeId, DisputeStatus status);

  @EntityGraph(
      attributePaths = {
        "grade",
        "grade.exam",
        "grade.exam.offering",
        "grade.exam.offering.course",
        "student",
        "student.user"
      })
  List<JGradeDispute> findByStudentId(UUID studentId);
}
