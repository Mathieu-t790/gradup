package app.mata.gradup.repository;

import app.mata.gradup.repository.model.JCourseOffering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseOfferingRepository extends JpaRepository<JCourseOffering, UUID> {

  @Override
  @EntityGraph(attributePaths = {"course", "group", "semester", "semester.academicYear"})
  Optional<JCourseOffering> findById(UUID id);

  String OPTIONAL_FILTERS_WHERE =
      """
      where (cast(:semesterId as uuid) is null or o.semester.id = :semesterId)
        and (cast(:groupId as uuid) is null or o.group.id = :groupId)
        and (cast(:courseId as uuid) is null or o.course.id = :courseId)
      """;

  Page<JCourseOffering> findBySemesterId(UUID semesterId, Pageable pageable);

  Page<JCourseOffering> findByGroupId(UUID groupId, Pageable pageable);

  Page<JCourseOffering> findByCourseId(UUID courseId, Pageable pageable);

  @Query(
      value =
          """
          select o from JCourseOffering o
          join fetch o.course
          join fetch o.semester s
          join fetch s.academicYear
          where o.group.id = :groupId and o.semester.id in :semesterIds
          """,
      countQuery =
          """
          select count(o) from JCourseOffering o
          where o.group.id = :groupId and o.semester.id in :semesterIds
          """)
  Page<JCourseOffering> findByGroupIdAndSemesterIdIn(
      @Param("groupId") UUID groupId,
      @Param("semesterIds") Collection<UUID> semesterIds,
      Pageable pageable);

  @Query(
      """
      select o from JCourseOffering o
      join fetch o.course
      join fetch o.semester s
      join fetch s.academicYear
      where o.id in :ids
      """)
  List<JCourseOffering> findAllWithCourseAndSemesterByIds(@Param("ids") Collection<UUID> ids);

  @Query(
      value =
          """
          select o from JCourseOffering o
          join fetch o.course
          join fetch o.group
          join fetch o.semester s
          join fetch s.academicYear
          """
              + OPTIONAL_FILTERS_WHERE,
      countQuery = "select count(o) from JCourseOffering o " + OPTIONAL_FILTERS_WHERE)
  Page<JCourseOffering> findByOptionalFilters(
      @Param("semesterId") UUID semesterId,
      @Param("groupId") UUID groupId,
      @Param("courseId") UUID courseId,
      Pageable pageable);

  @Query(
      "select coalesce(sum(c.credits), 0) from JCourse c "
          + "where c.id in ("
          + "  select o.course.id from JCourseOffering o left join o.group.track t "
          + "  where o.semester.id = :semesterId and (t.id = :trackId or t is null)) "
          + "or c.id = :prospectiveCourseId")
  int sumCreditsBySemesterIdAndTrackId(
      @Param("semesterId") UUID semesterId,
      @Param("trackId") UUID trackId,
      @Param("prospectiveCourseId") UUID prospectiveCourseId);

  @Query(
      "select coalesce(sum(c.credits), 0) from JCourse c "
          + "where c.id in ("
          + "  select o.course.id from JCourseOffering o left join o.group.track t "
          + "  join o.semester s "
          + "  where s.academicYear.id = :academicYearId and (t.id = :trackId or t is null)) "
          + "or c.id = :prospectiveCourseId")
  int sumCreditsByAcademicYearIdAndTrackId(
      @Param("academicYearId") UUID academicYearId,
      @Param("trackId") UUID trackId,
      @Param("prospectiveCourseId") UUID prospectiveCourseId);

  boolean existsByCourseIdAndGroupIdAndSemesterId(UUID courseId, UUID groupId, UUID semesterId);
}
