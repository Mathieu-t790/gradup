package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.CourseOfferingCreateRequest;
import app.mata.gradup.endpoint.rest.model.CourseOfferingPageResponse;
import app.mata.gradup.endpoint.rest.model.CourseOfferingResponse;
import app.mata.gradup.endpoint.rest.model.ExamResponse;
import app.mata.gradup.exception.BusinessRuleException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.CourseOfferingMapper;
import app.mata.gradup.repository.CourseOfferingRepository;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.ExamRepository;
import app.mata.gradup.repository.GroupRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.TeacherAssignmentRepository;
import app.mata.gradup.repository.TeacherRepository;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JTeacherAssignment;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CourseOfferingService {

  private final CourseOfferingRepository courseOfferingRepository;
  private final TeacherAssignmentRepository teacherAssignmentRepository;
  private final TeacherRepository teacherRepository;
  private final ExamRepository examRepository;
  private final CourseRepository courseRepository;
  private final GroupRepository groupRepository;
  private final SemesterRepository semesterRepository;
  private final CourseOfferingMapper courseOfferingMapper;

  @Transactional(readOnly = true)
  public CourseOfferingResponse getCourseOffering(UUID offeringId) {
    var offering =
        courseOfferingRepository
            .findById(offeringId)
            .orElseThrow(() -> new NotFoundException("Course offering not found"));
    return courseOfferingMapper.toRest(
        offering, teacherAssignmentRepository.findByOfferingId(offering.getId()));
  }

  @Transactional
  public void assignTeacher(UUID offeringId, UUID teacherId) {
    var offering =
        courseOfferingRepository
            .findById(offeringId)
            .orElseThrow(() -> new NotFoundException("Course offering not found"));
    var teacher =
        teacherRepository
            .findById(teacherId)
            .orElseThrow(() -> new NotFoundException("Teacher not found"));
    if (teacherAssignmentRepository.existsByTeacherIdAndOfferingId(teacherId, offeringId)) {
      throw new ConflictException("Teacher is already assigned to this course offering");
    }
    teacherAssignmentRepository.save(
        JTeacherAssignment.builder().offering(offering).teacher(teacher).build());
  }

  @Transactional
  public void unassignTeacher(UUID offeringId, UUID teacherId) {
    courseOfferingRepository
        .findById(offeringId)
        .orElseThrow(() -> new NotFoundException("Course offering not found"));
    teacherRepository
        .findById(teacherId)
        .orElseThrow(() -> new NotFoundException("Teacher not found"));
    teacherAssignmentRepository.deleteByOfferingIdAndTeacherId(offeringId, teacherId);
  }

  @Transactional(readOnly = true)
  public List<ExamResponse> listOfferingExams(UUID offeringId) {
    courseOfferingRepository
        .findById(offeringId)
        .orElseThrow(() -> new NotFoundException("Course offering not found"));
    return examRepository.findByOfferingId(offeringId).stream()
        .map(courseOfferingMapper::toRest)
        .toList();
  }

  @Transactional(readOnly = true)
  public CourseOfferingPageResponse listCourseOfferings(
      UUID semesterId, UUID groupId, UUID courseId, Pageable pageable) {
    Page<JCourseOffering> page =
        courseOfferingRepository.findByOptionalFilters(semesterId, groupId, courseId, pageable);
    Map<UUID, List<JTeacherAssignment>> assignmentsByOffering =
        teacherAssignmentRepository
            .findByOfferingIdIn(page.getContent().stream().map(JCourseOffering::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(a -> a.getOffering().getId()));
    return new CourseOfferingPageResponse()
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .first(page.isFirst())
        .last(page.isLast())
        .content(
            page.getContent().stream()
                .map(
                    offering ->
                        courseOfferingMapper.toRest(
                            offering,
                            assignmentsByOffering.getOrDefault(offering.getId(), List.of())))
                .toList());
  }

  @Transactional
  public CourseOfferingResponse createCourseOffering(CourseOfferingCreateRequest request) {
    var course =
        courseRepository
            .findById(request.getCourseId())
            .orElseThrow(() -> new NotFoundException("Course not found"));
    var group =
        groupRepository
            .findById(request.getGroupId())
            .orElseThrow(() -> new NotFoundException("Group not found"));
    var semester =
        semesterRepository
            .findById(request.getSemesterId())
            .orElseThrow(() -> new NotFoundException("Semester not found"));
    if (course.getSemesterNumber() != semester.getNumber()) {
      throw new BusinessRuleException(
          "Course semester number "
              + course.getSemesterNumber()
              + " does not match semester number "
              + semester.getNumber());
    }
    if (courseOfferingRepository.existsByCourseIdAndGroupIdAndSemesterId(
        course.getId(), group.getId(), semester.getId())) {
      throw new ConflictException(
          "Course offering already exists for this course, group and semester");
    }
    var track = group.getTrack();
    if (track != null) {
      int semesterCredits =
          courseOfferingRepository.sumCreditsBySemesterIdAndTrackId(semester.getId(), track.getId())
              + course.getCredits();
      if (semesterCredits > MAX_SEMESTER_CREDITS) {
        throw new BusinessRuleException(
            "Total credits for this semester and track would exceed " + MAX_SEMESTER_CREDITS);
      }
      int yearCredits =
          courseOfferingRepository.sumCreditsByAcademicYearIdAndTrackId(
                  semester.getAcademicYear().getId(), track.getId())
              + course.getCredits();
      if (yearCredits > MAX_YEAR_CREDITS) {
        throw new BusinessRuleException(
            "Total credits for this academic year and track would exceed " + MAX_YEAR_CREDITS);
      }
    }
    var offering =
        courseOfferingRepository.save(
            JCourseOffering.builder()
                .course(course)
                .group(group)
                .semester(semester)
                .gradingFinalized(false)
                .build());
    return courseOfferingMapper.toRest(offering, List.of());
  }

  private static final int MAX_SEMESTER_CREDITS = 30;
  private static final int MAX_YEAR_CREDITS = 60;
}
