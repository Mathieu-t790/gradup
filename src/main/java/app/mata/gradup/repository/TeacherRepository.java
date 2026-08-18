package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JTeacher;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends JpaRepository<JTeacher, UUID> {

  @EntityGraph(attributePaths = "user")
  @Override
  List<JTeacher> findAll();
}
