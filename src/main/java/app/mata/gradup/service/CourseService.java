package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.CourseCreateRequest;
import app.mata.gradup.endpoint.rest.model.CourseResponse;
import app.mata.gradup.endpoint.rest.model.CourseUpdateRequest;
import app.mata.gradup.exception.BadRequestException;
import app.mata.gradup.exception.ConflictException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.mapper.CourseMapper;
import app.mata.gradup.repository.CourseRepository;
import app.mata.gradup.repository.TrackRepository;
import app.mata.gradup.repository.model.JCourse;
import app.mata.gradup.repository.model.JTrack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CourseService {

  private final CourseRepository courseRepository;
  private final TrackRepository trackRepository;
  private final CourseMapper courseMapper;

  @Transactional(readOnly = true)
  public List<CourseResponse> listCourses(UUID trackId, Integer semesterNumber) {
    return findCourses(trackId, semesterNumber).stream()
        .map(courseMapper::toDomain)
        .map(courseMapper::toRest)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<JCourse> listForWeb() {
    return courseRepository.findAll();
  }

  @Transactional
  public CourseResponse createCourse(CourseCreateRequest request) {
    validateCreate(request);
    if (courseRepository.findByReference(request.getReference()).isPresent()) {
      throw new ConflictException(
          "A course with reference " + request.getReference() + " already exists");
    }
    var track = resolveTrack(request.getTrackId_JsonNullable());
    JCourse saved;
    try {
      saved =
          courseRepository.saveAndFlush(
              courseMapper.toEntity(courseMapper.toDomain(request, track)));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          "A course with reference " + request.getReference() + " already exists");
    }
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public CourseResponse getCourse(UUID courseId) {
    return toResponse(findCourse(courseId));
  }

  @Transactional
  public CourseResponse updateCourse(UUID courseId, CourseUpdateRequest request) {
    var course = findCourse(courseId);
    if (request.getTitle() != null) {
      if (request.getTitle().isBlank()) {
        throw new BadRequestException("Course title must not be blank");
      }
      course.setTitle(request.getTitle());
    }
    if (request.getCredits() != null) {
      if (request.getCredits() < 1) {
        throw new BadRequestException("Course credits must be at least 1");
      }
      course.setCredits(request.getCredits());
    }
    if (request.getTrackId_JsonNullable().isPresent()) {
      var trackId = request.getTrackId_JsonNullable().get();
      course.setTrack(trackId == null ? null : findTrack(trackId));
    }
    return toResponse(course);
  }

  private List<JCourse> findCourses(UUID trackId, Integer semesterNumber) {
    if (trackId != null && semesterNumber != null) {
      var courses = new ArrayList<JCourse>();
      courses.addAll(courseRepository.findBySemesterNumberAndTrackId(semesterNumber, trackId));
      courses.addAll(courseRepository.findBySemesterNumberAndTrackIsNull(semesterNumber));
      return courses;
    }
    if (trackId != null) {
      var courses = new ArrayList<JCourse>();
      courses.addAll(courseRepository.findByTrackId(trackId));
      courses.addAll(courseRepository.findByTrackIdIsNull());
      return courses;
    }
    if (semesterNumber != null) {
      return courseRepository.findBySemesterNumber(semesterNumber);
    }
    return courseRepository.findAll();
  }

  private static void validateCreate(CourseCreateRequest request) {
    if (request.getReference() == null || request.getReference().isBlank()) {
      throw new BadRequestException("Course reference must not be blank");
    }
    if (request.getTitle() == null || request.getTitle().isBlank()) {
      throw new BadRequestException("Course title must not be blank");
    }
    if (request.getCredits() == null || request.getCredits() < 1) {
      throw new BadRequestException("Course credits must be at least 1");
    }
    if (request.getSemesterNumber() == null
        || request.getSemesterNumber() < 1
        || request.getSemesterNumber() > 6) {
      throw new BadRequestException("Course semester number must be between 1 and 6");
    }
  }

  private JTrack resolveTrack(JsonNullable<UUID> trackId) {
    if (trackId == null || !trackId.isPresent() || trackId.get() == null) {
      return null;
    }
    return findTrack(trackId.get());
  }

  private JTrack findTrack(UUID trackId) {
    return trackRepository
        .findById(trackId)
        .orElseThrow(() -> new NotFoundException("Track not found"));
  }

  private JCourse findCourse(UUID courseId) {
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new NotFoundException("Course not found"));
  }

  private CourseResponse toResponse(JCourse course) {
    return courseMapper.toRest(courseMapper.toDomain(course));
  }
}
