package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JStudentTrackHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentTrackHistoryRepository extends JpaRepository<JStudentTrackHistory, UUID> {

  List<JStudentTrackHistory> findByStudentIdOrderByStartDateDesc(UUID studentId);

  List<JStudentTrackHistory> findByStudentIdOrderByStartDateAsc(UUID studentId);
}
