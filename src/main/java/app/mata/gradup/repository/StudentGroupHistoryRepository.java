package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JStudentGroupHistory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupHistoryRepository extends JpaRepository<JStudentGroupHistory, UUID> {

  @EntityGraph(attributePaths = {"group", "group.cohort", "group.track"})
  List<JStudentGroupHistory> findByStudentIdOrderByStartDateDesc(UUID studentId);

  @EntityGraph(attributePaths = {"group", "group.cohort", "group.track"})
  List<JStudentGroupHistory> findByStudentIdOrderByStartDateAsc(UUID studentId);

  @EntityGraph(attributePaths = {"group", "group.cohort", "group.track"})
  List<JStudentGroupHistory> findByStudentIdInAndEndDateIsNull(Collection<UUID> studentIds);

  Optional<JStudentGroupHistory> findFirstByStudentIdAndEndDateIsNull(UUID studentId);
}
