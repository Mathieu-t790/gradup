package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JStudent;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<JStudent, UUID> {

  @Override
  @EntityGraph(attributePaths = {"user", "cohort"})
  Page<JStudent> findAll(Pageable pageable);

  @EntityGraph(attributePaths = {"user", "cohort"})
  Page<JStudent> findByCohortId(UUID cohortId, Pageable pageable);

  @Query(
      """
      SELECT s FROM JStudent s
      WHERE EXISTS (
            SELECT h FROM JStudentGroupHistory h
            WHERE h.student = s AND h.group.id = :groupId AND h.endDate IS NULL)
      """)
  @EntityGraph(attributePaths = {"user", "cohort"})
  Page<JStudent> findByCurrentGroupId(UUID groupId, Pageable pageable);

  @Query(
      """
      SELECT s FROM JStudent s
      WHERE s.cohort.id = :cohortId
        AND EXISTS (
              SELECT h FROM JStudentGroupHistory h
              WHERE h.student = s AND h.group.id = :groupId AND h.endDate IS NULL)
      """)
  @EntityGraph(attributePaths = {"user", "cohort"})
  Page<JStudent> findByCohortIdAndCurrentGroupId(UUID cohortId, UUID groupId, Pageable pageable);
}
