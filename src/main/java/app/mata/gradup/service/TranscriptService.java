package app.mata.gradup.service;

import app.mata.gradup.endpoint.event.EventProducer;
import app.mata.gradup.endpoint.event.model.TranscriptGenerated;
import app.mata.gradup.endpoint.rest.model.TranscriptGenerateRequest;
import app.mata.gradup.endpoint.rest.model.TranscriptResponse;
import app.mata.gradup.exception.BadRequestException;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.mapper.TranscriptMapper;
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
import jakarta.transaction.Transactional;
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

    JTranscript transcript;
    List<JCourseOffering> offerings;
    TranscriptPdfData pdfData;

    switch (request.getType()) {
      case PROVISIONAL -> {
        JSemester semester = semester(request.getSemesterId_JsonNullable());
        JAcademicYear year = semester.getAcademicYear();
        offerings = scope.forSemester(student, semester);
        pdfData =
            pdfData(
                student,
                TranscriptScoring.title(student, year),
                TranscriptScoring.inscriptionLine(
                    student, year, scope.trackAt(student, LocalDate.now())),
                offerings,
                averageByOffering,
                null);
        transcript =
            baseTranscript(
                transcriptId,
                storageKey,
                student,
                transcriptType(request),
                semester,
                null,
                null,
                null,
                null,
                recipientEmail);
      }
      case FULL -> {
        JAcademicYear year = academicYear(request.getAcademicYearId_JsonNullable());
        offerings = scope.forYear(student, year);
        ResultInfo result = TranscriptScoring.score(offerings, averageByOffering);
        pdfData =
            pdfData(
                student,
                TranscriptScoring.title(student, year),
                TranscriptScoring.inscriptionLine(
                    student, year, scope.trackAt(student, LocalDate.now())),
                offerings,
                averageByOffering,
                result);
        transcript =
            baseTranscript(
                transcriptId,
                storageKey,
                student,
                transcriptType(request),
                null,
                year,
                null,
                result.creditsAcquired(),
                result.weightedAverage(),
                recipientEmail);
      }
      case DIPLOMA -> {
        JDiploma diploma = diploma(studentId, request.getDiplomaId_JsonNullable());
        offerings = scope.byIds(averageByOffering.keySet());
        ResultInfo scored = TranscriptScoring.score(offerings, averageByOffering);
        ResultInfo result =
            new ResultInfo(
                scored.creditsAcquired(), scored.totalCredits(), diploma.getOverallAverage());
        pdfData =
            pdfData(
                student,
                "Relevé de notes",
                TranscriptScoring.diplomaInscriptionLine(diploma),
                offerings,
                averageByOffering,
                result);
        transcript =
            baseTranscript(
                transcriptId,
                storageKey,
                student,
                transcriptType(request),
                null,
                null,
                diploma,
                result.creditsAcquired(),
                diploma.getOverallAverage(),
                recipientEmail);
      }
      default -> throw new BadRequestException("Unsupported transcript type: " + request.getType());
    }

    File pdf = pdfRenderer.render(pdfData);
    bucketComponent.upload(pdf, storageKey);

    transcript = transcriptRepository.save(transcript);
    transcriptDetailRepository.saveAll(details(offerings, transcript, averageByOffering));

    dispatchEmailEvent(transcriptId);

    String downloadUrl = bucketComponent.presign(storageKey, Duration.ofHours(1)).toString();
    return transcriptMapper.toRest(transcript, downloadUrl);
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
            pageable -> vCourseAverageRepository.findByStudentId(studentId, pageable), 200)
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

  private JTranscript baseTranscript(
      UUID id,
      String storageKey,
      JStudent student,
      app.mata.gradup.model.TranscriptType type,
      JSemester semester,
      JAcademicYear academicYear,
      JDiploma diploma,
      Integer creditsEarned,
      BigDecimal overallAverage,
      String recipientEmail) {
    return JTranscript.builder()
        .id(id)
        .storageKey(storageKey)
        .student(student)
        .type(type)
        .semester(semester)
        .academicYear(academicYear)
        .diploma(diploma)
        .creditsEarned(creditsEarned)
        .overallAverage(overallAverage)
        .recipientEmail(recipientEmail)
        .build();
  }

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

  private app.mata.gradup.model.TranscriptType transcriptType(TranscriptGenerateRequest request) {
    return app.mata.gradup.model.TranscriptType.valueOf(request.getType().name());
  }

  private static <T> T nullableOrNull(JsonNullable<T> value) {
    return value == null ? null : value.orElse(null);
  }

  private static <T> boolean present(JsonNullable<T> value) {
    return value != null && value.isPresent();
  }
}
