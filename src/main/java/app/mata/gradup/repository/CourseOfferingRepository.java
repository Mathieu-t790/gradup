package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JCourseOffering;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseOfferingRepository extends JpaRepository<JCourseOffering, UUID> {

  Page<JCourseOffering> findBySemesterId(UUID semesterId, Pageable pageable);

  Page<JCourseOffering> findByGroupId(UUID groupId, Pageable pageable);

  Page<JCourseOffering> findByCourseId(UUID courseId, Pageable pageable);
}
