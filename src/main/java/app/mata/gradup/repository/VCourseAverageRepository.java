package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JVCourseAverage;
import app.mata.gradup.repository.model.JVCourseAverage.CourseAverageId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VCourseAverageRepository extends JpaRepository<JVCourseAverage, CourseAverageId> {

  Page<JVCourseAverage> findByStudentId(UUID studentId, Pageable pageable);
}
