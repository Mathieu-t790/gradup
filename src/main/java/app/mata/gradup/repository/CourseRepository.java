package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JCourse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<JCourse, UUID> {

  Optional<JCourse> findByReference(String reference);

  @EntityGraph(attributePaths = {"track"})
  List<JCourse> findByTrackId(UUID trackId);

  @EntityGraph(attributePaths = {"track"})
  List<JCourse> findByTrackIdIsNull();

  @EntityGraph(attributePaths = {"track"})
  List<JCourse> findBySemesterNumber(int semesterNumber);

  @EntityGraph(attributePaths = {"track"})
  List<JCourse> findBySemesterNumberAndTrackId(int semesterNumber, UUID trackId);

  @EntityGraph(attributePaths = {"track"})
  List<JCourse> findBySemesterNumberAndTrackIsNull(int semesterNumber);
}
