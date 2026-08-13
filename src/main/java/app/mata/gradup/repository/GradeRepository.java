package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JGrade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<JGrade, UUID> {

  Page<JGrade> findByStudentId(UUID studentId, Pageable pageable);

  List<JGrade> findByExamId(UUID examId);

  Optional<JGrade> findByStudentIdAndExamId(UUID studentId, UUID examId);
}
