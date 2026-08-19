package app.mata.gradup.service.event;

import app.mata.gradup.endpoint.event.model.TranscriptGenerated;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.mail.Email;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.model.JTranscript;
import app.mata.gradup.service.utils.EmailAssets;
import app.mata.gradup.service.utils.HtmlTemplater;
import app.mata.gradup.service.utils.Users;
import app.mata.gradup.service.utils.Wording;
import jakarta.mail.internet.InternetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptGeneratedService implements Consumer<TranscriptGenerated> {

  private static final Duration DOWNLOAD_URL_EXPIRATION = Duration.ofDays(3);

  private final TranscriptRepository transcriptRepository;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;
  private final HtmlTemplater htmlTemplater;

  @Override
  @Transactional
  public void accept(TranscriptGenerated event) {
    JTranscript transcript =
        transcriptRepository
            .findById(event.getTranscriptId())
            .orElseThrow(
                () -> new NotFoundException("Transcript not found: " + event.getTranscriptId()));
    mailer.accept(emailOf(transcript));
    transcript.setSentAt(Instant.now());
    transcriptRepository.save(transcript);
  }

  private Email emailOf(JTranscript transcript) {
    try {
      String reference = transcript.getStudent().getUser().getReference();
      String studentName = Users.fullName(transcript.getStudent().getUser());
      String downloadUrl =
          bucketComponent.presign(transcript.getStorageKey(), DOWNLOAD_URL_EXPIRATION).toString();
      Context context = new Context();
      context.setVariable("studentName", studentName);
      context.setVariable("downloadUrl", downloadUrl);
      context.setVariable("signatureDataUri", EmailAssets.SIGNATURE_DATA_URI);
      String htmlBody = htmlTemplater.render("email/transcript", context);
      return new Email(
          new InternetAddress(transcript.getRecipientEmail()),
          List.of(),
          List.of(),
          Wording.get("transcript.subject", reference),
          htmlBody,
          List.of());
    } catch (Exception e) {
      throw new RuntimeException("Could not build email for transcript " + transcript.getId(), e);
    }
  }
}
