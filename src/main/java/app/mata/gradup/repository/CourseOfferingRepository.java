package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JCourseOffering;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseOfferingRepository extends JpaRepository<JCourseOffering, UUID> {

  Page<JCourseOffering> findBySemesterId(UUID semesterId, Pageable pageable);

  Page<JCourseOffering> findByGroupId(UUID groupId, Pageable pageable);

  Page<JCourseOffering> findByCourseId(UUID courseId, Pageable pageable);

  Page<JCourseOffering> findByGroupIdAndSemesterIdIn(
      UUID groupId, Collection<UUID> semesterIds, Pageable pageable);

  @Query(
      """
      select o from JCourseOffering o
      where (cast(:semesterId as uuid) is null or o.semester.id = :semesterId)
        and (cast(:groupId as uuid) is null or o.group.id = :groupId)
        and (cast(:courseId as uuid) is null or o.course.id = :courseId)
      """)
  Page<JCourseOffering> findByOptionalFilters(
      @Param("semesterId") UUID semesterId,
      @Param("groupId") UUID groupId,
      @Param("courseId") UUID courseId,
      Pageable pageable);

  @Query(
      "select coalesce(sum(c.credits), 0) from JCourseOffering o join o.course c "
          + "where o.semester.id = :semesterId and o.group.track.id = :trackId")
  int sumCreditsBySemesterIdAndTrackId(
      @Param("semesterId") UUID semesterId, @Param("trackId") UUID trackId);

  @Query(
      "select coalesce(sum(c.credits), 0) from JCourseOffering o join o.course c "
          + "join o.semester s where s.academicYear.id = :academicYearId "
          + "and o.group.track.id = :trackId")
  int sumCreditsByAcademicYearIdAndTrackId(
      @Param("academicYearId") UUID academicYearId, @Param("trackId") UUID trackId);

  boolean existsByCourseIdAndGroupIdAndSemesterId(UUID courseId, UUID groupId, UUID semesterId);
}
