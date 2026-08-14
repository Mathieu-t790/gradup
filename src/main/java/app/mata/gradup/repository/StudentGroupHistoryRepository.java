package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JStudentGroupHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupHistoryRepository extends JpaRepository<JStudentGroupHistory, UUID> {

  List<JStudentGroupHistory> findByStudentIdOrderByStartDateDesc(UUID studentId);

  List<JStudentGroupHistory> findByStudentIdOrderByStartDateAsc(UUID studentId);
}
