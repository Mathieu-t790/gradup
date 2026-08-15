package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JGrade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<JGrade, UUID> {

  @Query(
      """
      SELECT g FROM JGrade g
      JOIN FETCH g.student st
      JOIN FETCH st.user
      JOIN FETCH g.exam e
      JOIN FETCH e.offering o
      JOIN FETCH o.course
      WHERE g.student.id = :studentId
      """)
  Page<JGrade> findByStudentId(UUID studentId, Pageable pageable);

  @Query(
      """
      SELECT g FROM JGrade g
      JOIN FETCH g.student st
      JOIN FETCH st.user
      JOIN FETCH g.exam e
      JOIN FETCH e.offering o
      JOIN FETCH o.course
      WHERE g.student.id = :studentId AND o.semester.id = :semesterId
      """)
  Page<JGrade> findByStudentIdAndSemesterId(UUID studentId, UUID semesterId, Pageable pageable);

  List<JGrade> findByExamId(UUID examId);

  Optional<JGrade> findByStudentIdAndExamId(UUID studentId, UUID examId);
}
