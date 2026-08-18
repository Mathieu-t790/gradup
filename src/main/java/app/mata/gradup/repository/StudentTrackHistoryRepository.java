package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JStudentTrackHistory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentTrackHistoryRepository extends JpaRepository<JStudentTrackHistory, UUID> {

  @EntityGraph(attributePaths = {"track"})
  List<JStudentTrackHistory> findByStudentIdOrderByStartDateDesc(UUID studentId);

  @EntityGraph(attributePaths = {"track"})
  List<JStudentTrackHistory> findByStudentIdOrderByStartDateAsc(UUID studentId);

  @EntityGraph(attributePaths = {"track"})
  List<JStudentTrackHistory> findByStudentIdInAndEndDateIsNull(Collection<UUID> studentIds);

  Optional<JStudentTrackHistory> findFirstByStudentIdAndEndDateIsNull(UUID studentId);
}
