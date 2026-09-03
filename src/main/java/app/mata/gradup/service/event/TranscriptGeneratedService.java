package app.mata.gradup.service.event;

import app.mata.gradup.endpoint.event.model.TranscriptGenerated;
import app.mata.gradup.exception.NotFoundException;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.mail.Email;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.model.JTranscript;
import app.mata.gradup.service.TranscriptService;
import app.mata.gradup.service.utils.DownloadPresigner;
import app.mata.gradup.service.utils.EmailAssets;
import app.mata.gradup.service.utils.HtmlTemplater;
import app.mata.gradup.service.utils.PdfRenderer;
import app.mata.gradup.service.utils.Users;
import app.mata.gradup.service.utils.Wording;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
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
  private final PdfRenderer pdfRenderer;
  private final BucketComponent bucketComponent;
  private final DownloadPresigner downloadPresigner;
  private final Mailer mailer;
  private final HtmlTemplater htmlTemplater;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void accept(TranscriptGenerated event) {
    JTranscript transcript =
        transcriptRepository
            .findById(event.getTranscriptId())
            .orElseThrow(
                () -> new NotFoundException("Transcript not found: " + event.getTranscriptId()));

    if (event.getPdfData() != null) {
      renderAndUpload(transcript, event.getPdfData());
    }

    mailer.accept(emailOf(transcript));
    transcript.setSentAt(Instant.now());
    transcriptRepository.save(transcript);
  }

  private void renderAndUpload(JTranscript transcript, String pdfDataJson) {
    try {
      PdfRenderer.TranscriptPdfData pdfData =
          objectMapper.readValue(pdfDataJson, PdfRenderer.TranscriptPdfData.class);
      File pdf = pdfRenderer.render(pdfData);
      bucketComponent.upload(pdf, transcript.getStorageKey());
    } catch (Exception e) {
      throw new RuntimeException("Could not render transcript PDF " + transcript.getId(), e);
    }
  }

  private Email emailOf(JTranscript transcript) {
    try {
      String reference = transcript.getStudent().getUser().getReference();
      String studentName = Users.fullName(transcript.getStudent().getUser());
      String downloadUrl =
          downloadPresigner
              .presign(
                  transcript.getStorageKey(),
                  DOWNLOAD_URL_EXPIRATION,
                  TranscriptService.buildDownloadFilename(transcript))
              .toString();
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
