package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JSemester;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterRepository extends JpaRepository<JSemester, UUID> {

  @EntityGraph(attributePaths = {"academicYear"})
  List<JSemester> findByAcademicYearId(UUID academicYearId);

  @Override
  @EntityGraph(attributePaths = {"academicYear"})
  List<JSemester> findAll();

  boolean existsByNumberAndAcademicYearId(int number, UUID academicYearId);
}
