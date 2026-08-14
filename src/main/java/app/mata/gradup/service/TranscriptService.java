package app.mata.gradup.service;

import app.mata.gradup.endpoint.event.EventProducer;
import app.mata.gradup.endpoint.event.model.TranscriptGenerated;
import app.mata.gradup.endpoint.rest.model.TranscriptGenerateRequest;
import app.mata.gradup.endpoint.rest.model.TranscriptResponse;
import app.mata.gradup.exception.BadRequestException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.mapper.TranscriptMapper;
import app.mata.gradup.model.TranscriptType;
import app.mata.gradup.repository.AcademicYearRepository;
import app.mata.gradup.repository.DiplomaRepository;
import app.mata.gradup.repository.SemesterRepository;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.TranscriptDetailRepository;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.VCourseAverageRepository;
import app.mata.gradup.repository.model.JAcademicYear;
import app.mata.gradup.repository.model.JCourseOffering;
import app.mata.gradup.repository.model.JDiploma;
import app.mata.gradup.repository.model.JSemester;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTranscript;
import app.mata.gradup.repository.model.JTranscriptDetail;
import app.mata.gradup.repository.model.JVCourseAverage;
import app.mata.gradup.service.utils.Pages;
import app.mata.gradup.service.utils.PdfRenderer;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.AbsencesInfo;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.ResultInfo;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.StudentInfo;
import app.mata.gradup.service.utils.TranscriptScope;
import app.mata.gradup.service.utils.TranscriptScoring;
import app.mata.gradup.service.utils.Wording;
import java.io.File;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptService {

  private final TranscriptRepository transcriptRepository;
  private final TranscriptDetailRepository transcriptDetailRepository;
  private final StudentRepository studentRepository;
  private final SemesterRepository semesterRepository;
  private final AcademicYearRepository academicYearRepository;
  private final VCourseAverageRepository vCourseAverageRepository;
  private final DiplomaRepository diplomaRepository;
  private final TranscriptScope scope;
  private final BucketComponent bucketComponent;
  private final PdfRenderer pdfRenderer;
  private final EventProducer eventProducer;
  private final TranscriptMapper transcriptMapper;

  @Transactional
  public TranscriptResponse generateStudentTranscript(
      UUID studentId, TranscriptGenerateRequest request) {
    validateRequest(request);
    JStudent student = student(studentId);
    String recipientEmail = emailOf(request, student);
    UUID transcriptId = UUID.randomUUID();
    String storageKey = "transcripts/" + studentId + "/" + transcriptId + ".pdf";
    Map<UUID, BigDecimal> averageByOffering = averagesByOffering(studentId);

    PreparedTranscript prepared =
        prepare(request, student, transcriptId, storageKey, recipientEmail, averageByOffering);

    File pdf = pdfRenderer.render(prepared.pdfData());
    bucketComponent.upload(pdf, storageKey);

    JTranscript transcript = transcriptRepository.save(prepared.transcript());
    transcriptDetailRepository.saveAll(
        details(prepared.offerings(), transcript, averageByOffering));

    dispatchEmailEvent(transcriptId);

    String downloadUrl = bucketComponent.presign(storageKey, Duration.ofHours(1)).toString();
    return transcriptMapper.toRest(transcript, downloadUrl);
  }

  private PreparedTranscript prepare(
      TranscriptGenerateRequest request,
      JStudent student,
      UUID transcriptId,
      String storageKey,
      String recipientEmail,
      Map<UUID, BigDecimal> averageByOffering) {
    return switch (request.getType()) {
      case PROVISIONAL ->
          prepareProvisional(
              request, student, transcriptId, storageKey, recipientEmail, averageByOffering);
      case FULL ->
          prepareFull(
              request, student, transcriptId, storageKey, recipientEmail, averageByOffering);
      case DIPLOMA ->
          prepareDiploma(
              request, student, transcriptId, storageKey, recipientEmail, averageByOffering);
      default -> throw new BadRequestException("Unsupported transcript type: " + request.getType());
    };
  }

  private PreparedTranscript prepareProvisional(
      TranscriptGenerateRequest request,
      JStudent student,
      UUID transcriptId,
      String storageKey,
      String recipientEmail,
      Map<UUID, BigDecimal> averageByOffering) {
    JSemester semester = semester(request.getSemesterId_JsonNullable());
    JAcademicYear year = semester.getAcademicYear();
    List<JCourseOffering> offerings = scope.forSemester(student, semester);
    TranscriptPdfData pdfData =
        pdfData(
            student,
            TranscriptScoring.title(student, year),
            TranscriptScoring.inscriptionLine(
                student, year, scope.trackAt(student, LocalDate.now())),
            offerings,
            averageByOffering,
            null);
    JTranscript transcript =
        JTranscript.builder()
            .id(transcriptId)
            .storageKey(storageKey)
            .student(student)
            .type(transcriptType(request))
            .semester(semester)
            .recipientEmail(recipientEmail)
            .build();
    return new PreparedTranscript(transcript, pdfData, offerings);
  }

  private PreparedTranscript prepareFull(
      TranscriptGenerateRequest request,
      JStudent student,
      UUID transcriptId,
      String storageKey,
      String recipientEmail,
      Map<UUID, BigDecimal> averageByOffering) {
    JAcademicYear year = academicYear(request.getAcademicYearId_JsonNullable());
    List<JCourseOffering> offerings = scope.forYear(student, year);
    ResultInfo result = TranscriptScoring.score(offerings, averageByOffering);
    TranscriptPdfData pdfData =
        pdfData(
            student,
            TranscriptScoring.title(student, year),
            TranscriptScoring.inscriptionLine(
                student, year, scope.trackAt(student, LocalDate.now())),
            offerings,
            averageByOffering,
            result);
    JTranscript transcript =
        JTranscript.builder()
            .id(transcriptId)
            .storageKey(storageKey)
            .student(student)
            .type(transcriptType(request))
            .academicYear(year)
            .creditsEarned(result.creditsAcquired())
            .overallAverage(result.weightedAverage())
            .recipientEmail(recipientEmail)
            .build();
    return new PreparedTranscript(transcript, pdfData, offerings);
  }

  private PreparedTranscript prepareDiploma(
      TranscriptGenerateRequest request,
      JStudent student,
      UUID transcriptId,
      String storageKey,
      String recipientEmail,
      Map<UUID, BigDecimal> averageByOffering) {
    JDiploma diploma = diploma(student.getId(), request.getDiplomaId_JsonNullable());
    List<JCourseOffering> offerings = scope.byIds(averageByOffering.keySet());
    ResultInfo scored = TranscriptScoring.score(offerings, averageByOffering);
    ResultInfo result =
        new ResultInfo(
            scored.creditsAcquired(), scored.totalCredits(), diploma.getOverallAverage());
    TranscriptPdfData pdfData =
        pdfData(
            student,
            Wording.get("transcript.title"),
            TranscriptScoring.diplomaInscriptionLine(diploma),
            offerings,
            averageByOffering,
            result);
    JTranscript transcript =
        JTranscript.builder()
            .id(transcriptId)
            .storageKey(storageKey)
            .student(student)
            .type(transcriptType(request))
            .diploma(diploma)
            .creditsEarned(result.creditsAcquired())
            .overallAverage(diploma.getOverallAverage())
            .recipientEmail(recipientEmail)
            .build();
    return new PreparedTranscript(transcript, pdfData, offerings);
  }

  @Transactional
  public List<TranscriptResponse> listStudentTranscripts(UUID studentId) {
    studentRepository
        .findById(studentId)
        .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
    return transcriptRepository.findByStudentIdOrderByGeneratedAtDesc(studentId).stream()
        .map(
            transcript ->
                transcriptMapper.toRest(transcript, presignUrl(transcript.getStorageKey())))
        .toList();
  }

  private void dispatchEmailEvent(UUID transcriptId) {
    try {
      eventProducer.accept(List.of(new TranscriptGenerated(transcriptId)));
    } catch (Exception e) {
      log.warn("Could not dispatch transcript email event for transcript {}", transcriptId, e);
    }
  }

  private String presignUrl(String storageKey) {
    if (storageKey == null) {
      return null;
    }
    return bucketComponent.presign(storageKey, Duration.ofMinutes(30)).toString();
  }

  private void validateRequest(TranscriptGenerateRequest request) {
    boolean valid =
        switch (request.getType()) {
          case PROVISIONAL ->
              present(request.getSemesterId_JsonNullable())
                  && !present(request.getAcademicYearId_JsonNullable())
                  && !present(request.getDiplomaId_JsonNullable());
          case FULL ->
              present(request.getAcademicYearId_JsonNullable())
                  && !present(request.getSemesterId_JsonNullable())
                  && !present(request.getDiplomaId_JsonNullable());
          case DIPLOMA ->
              present(request.getDiplomaId_JsonNullable())
                  && !present(request.getSemesterId_JsonNullable())
                  && !present(request.getAcademicYearId_JsonNullable());
        };
    if (!valid) {
      throw new BadRequestException(
          "type "
              + request.getType()
              + " requires exactly one matching id: PROVISIONAL -> semesterId, "
              + "FULL -> academicYearId, DIPLOMA -> diplomaId");
    }
  }

  private JStudent student(UUID studentId) {
    return studentRepository
        .findById(studentId)
        .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
  }

  private JSemester semester(JsonNullable<UUID> semesterId) {
    return semesterRepository
        .findById(requiredId(semesterId, "semesterId"))
        .orElseThrow(() -> new NotFoundException("Semester not found: " + semesterId));
  }

  private JAcademicYear academicYear(JsonNullable<UUID> academicYearId) {
    return academicYearRepository
        .findById(requiredId(academicYearId, "academicYearId"))
        .orElseThrow(() -> new NotFoundException("Academic year not found: " + academicYearId));
  }

  private JDiploma diploma(UUID studentId, JsonNullable<UUID> diplomaId) {
    JDiploma diploma =
        diplomaRepository
            .findById(requiredId(diplomaId, "diplomaId"))
            .orElseThrow(() -> new NotFoundException("Diploma not found: " + diplomaId));
    if (!diploma.getStudent().getId().equals(studentId)) {
      throw new BadRequestException(
          "Diploma " + diploma.getId() + " does not belong to student " + studentId);
    }
    return diploma;
  }

  private UUID requiredId(JsonNullable<UUID> id, String field) {
    UUID value = nullableOrNull(id);
    if (value == null) {
      throw new BadRequestException("Missing required id: " + field);
    }
    return value;
  }

  private String emailOf(TranscriptGenerateRequest request, JStudent student) {
    return request.getRecipientEmail() == null || request.getRecipientEmail().isBlank()
        ? student.getUser().getEmail()
        : request.getRecipientEmail();
  }

  private Map<UUID, BigDecimal> averagesByOffering(UUID studentId) {
    return Pages.allPages(
            pageable -> vCourseAverageRepository.findByStudentId(studentId, pageable),
            Pages.DEFAULT_PAGE_SIZE)
        .stream()
        .collect(
            Collectors.toMap(
                JVCourseAverage::getOfferingId, JVCourseAverage::getAverage, (a, b) -> a));
  }

  private TranscriptPdfData pdfData(
      JStudent student,
      String title,
      String inscriptionLine,
      List<JCourseOffering> offerings,
      Map<UUID, BigDecimal> averageByOffering,
      ResultInfo result) {
    StudentInfo studentInfo =
        new StudentInfo(
            student.getUser().getLastName(),
            student.getUser().getFirstName(),
            student.getUser().getReference(),
            inscriptionLine);
    return new TranscriptPdfData(
        title,
        studentInfo,
        TranscriptScoring.courseLines(offerings, averageByOffering),
        new AbsencesInfo(null, null, null),
        result);
  }

  private record PreparedTranscript(
      JTranscript transcript, TranscriptPdfData pdfData, List<JCourseOffering> offerings) {}

  private List<JTranscriptDetail> details(
      List<JCourseOffering> offerings,
      JTranscript transcript,
      Map<UUID, BigDecimal> averageByOffering) {
    return offerings.stream()
        .map(
            offering ->
                JTranscriptDetail.builder()
                    .transcript(transcript)
                    .offering(offering)
                    .courseScore(averageByOffering.get(offering.getId()))
                    .creditsEarned(
                        TranscriptScoring.hasPassed(averageByOffering.get(offering.getId())))
                    .build())
        .toList();
  }

  private TranscriptType transcriptType(TranscriptGenerateRequest request) {
    return TranscriptType.valueOf(request.getType().name());
  }

  private static <T> T nullableOrNull(JsonNullable<T> value) {
    return value == null ? null : value.orElse(null);
  }

  private static <T> boolean present(JsonNullable<T> value) {
    return value != null && value.isPresent();
  }
}
