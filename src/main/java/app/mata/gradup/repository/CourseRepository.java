package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JCourse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<JCourse, UUID> {

  Optional<JCourse> findByReference(String reference);

  List<JCourse> findByTrackId(UUID trackId);

  List<JCourse> findByTrackIdIsNull();

  List<JCourse> findBySemesterNumber(int semesterNumber);

  List<JCourse> findBySemesterNumberAndTrackId(int semesterNumber, UUID trackId);

  List<JCourse> findBySemesterNumberAndTrackIsNull(int semesterNumber);
}
