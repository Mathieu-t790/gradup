package app.mata.gradup.service;

import app.mata.gradup.endpoint.rest.model.TranscriptResponse;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.mapper.TranscriptMapper;
import app.mata.gradup.repository.StudentRepository;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.model.JTranscript;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class StudentTranscriptService {

  private static final Duration DOWNLOAD_URL_EXPIRATION = Duration.ofHours(1);

  private final StudentRepository studentRepository;
  private final TranscriptRepository transcriptRepository;
  private final TranscriptMapper transcriptMapper;
  private final BucketComponent bucketComponent;

  @Transactional(readOnly = true)
  public List<TranscriptResponse> listStudentTranscripts(UUID studentId) {
    requireStudent(studentId);
    return transcriptRepository.findByStudentId(studentId).stream()
        .sorted(Comparator.comparing(JTranscript::getGeneratedAt).reversed())
        .map(this::toRest)
        .toList();
  }

  private TranscriptResponse toRest(JTranscript entity) {
    return transcriptMapper.toRest(
        transcriptMapper.toDomain(entity, downloadUrl(entity.getStorageKey())));
  }

  private String downloadUrl(String storageKey) {
    if (storageKey == null) {
      return null;
    }
    return bucketComponent.presign(storageKey, DOWNLOAD_URL_EXPIRATION).toString();
  }

  private void requireStudent(UUID studentId) {
    if (!studentRepository.existsById(studentId)) {
      throw new NotFoundException("Student not found");
    }
  }
}
