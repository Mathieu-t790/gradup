package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JVCourseAverage;
import app.mata.gradup.repository.model.JVCourseAverage.CourseAverageId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VCourseAverageRepository extends JpaRepository<JVCourseAverage, CourseAverageId> {

  List<JVCourseAverage> findByStudentId(UUID studentId);
}
