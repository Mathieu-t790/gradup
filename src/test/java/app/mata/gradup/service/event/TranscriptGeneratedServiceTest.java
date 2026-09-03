package app.mata.gradup.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.mata.gradup.endpoint.event.model.TranscriptGenerated;
import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.mail.Email;
import app.mata.gradup.mail.Mailer;
import app.mata.gradup.model.Role;
import app.mata.gradup.repository.TranscriptRepository;
import app.mata.gradup.repository.model.JStudent;
import app.mata.gradup.repository.model.JTranscript;
import app.mata.gradup.repository.model.JUser;
import app.mata.gradup.service.utils.DownloadPresigner;
import app.mata.gradup.service.utils.HtmlTemplater;
import app.mata.gradup.service.utils.PdfRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

class TranscriptGeneratedServiceTest {

  private final TranscriptRepository transcriptRepository = mock(TranscriptRepository.class);
  private final PdfRenderer pdfRenderer = mock(PdfRenderer.class);
  private final BucketComponent bucketComponent = mock(BucketComponent.class);
  private final DownloadPresigner downloadPresigner = mock(DownloadPresigner.class);
  private final Mailer mailer = mock(Mailer.class);
  private final HtmlTemplater htmlTemplater = mock(HtmlTemplater.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final TranscriptGeneratedService service =
      new TranscriptGeneratedService(
          transcriptRepository,
          pdfRenderer,
          bucketComponent,
          downloadPresigner,
          mailer,
          htmlTemplater,
          objectMapper);

  @Test
  void accept_emails_without_attachment_and_marks_sent() throws Exception {
    UUID transcriptId = UUID.randomUUID();
    JUser user =
        JUser.builder()
            .id(UUID.randomUUID())
            .firstName("Tafita")
            .lastName("Mathieu")
            .email("tafita@cu.te")
            .reference("STD21001")
            .role(Role.STUDENT)
            .build();
    JStudent student = JStudent.builder().id(UUID.randomUUID()).user(user).build();
    JTranscript transcript =
        JTranscript.builder()
            .id(transcriptId)
            .student(student)
            .storageKey("transcripts/" + student.getId() + "/" + transcriptId + ".pdf")
            .recipientEmail("tafita@cu.te")
            .build();
    when(transcriptRepository.findById(transcriptId)).thenReturn(Optional.of(transcript));
    when(downloadPresigner.presign(any(), any(), any()))
        .thenReturn(new URL("https://bucket.example/transcript.pdf?X-Amz-Expires=259200"));
    when(htmlTemplater.render(eq("email/transcript"), any(Context.class)))
        .thenReturn("<a href=\"https://bucket.example/transcript.pdf\">Télécharger</a>");

    service.accept(new TranscriptGenerated(transcriptId));

    ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
    ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(downloadPresigner)
        .presign(
            eq(transcript.getStorageKey()), durationCaptor.capture(), fileNameCaptor.capture());
    assertEquals(Duration.ofDays(3), durationCaptor.getValue());
    assertTrue(
        fileNameCaptor.getValue().startsWith("relevé_notes_STD21001_"),
        "download name must carry the student reference and the transcript prefix");

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    Email email = emailCaptor.getValue();
    assertTrue(email.attachments().isEmpty(), "email must not carry a PDF attachment");
    assertEquals("Relevé de notes – STD21001", email.subject());
    assertEquals("tafita@cu.te", email.to().getAddress());
    assertTrue(email.htmlBody().contains("Télécharger"), "body must contain the download button");

    verify(transcriptRepository).save(transcript);
    assertNotNull(transcript.getSentAt(), "transcript must be marked as sent");
  }

  @Test
  void accept_emails_without_downloading_the_pdf() throws Exception {
    UUID transcriptId = UUID.randomUUID();
    JTranscript transcript =
        JTranscript.builder()
            .id(transcriptId)
            .student(studentWithUser())
            .storageKey("transcripts/x.pdf")
            .recipientEmail("tafita@cu.te")
            .build();
    when(transcriptRepository.findById(transcriptId)).thenReturn(Optional.of(transcript));
    when(downloadPresigner.presign(any(), any(), any()))
        .thenReturn(new URL("https://bucket.example/x.pdf"));

    service.accept(new TranscriptGenerated(transcriptId));

    verify(downloadPresigner).presign(eq("transcripts/x.pdf"), any(), any());
    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    assertTrue(emailCaptor.getValue().attachments().isEmpty());
  }

  private static JStudent studentWithUser() {
    JUser user =
        JUser.builder()
            .id(UUID.randomUUID())
            .firstName("Tafita")
            .lastName("Mathieu")
            .email("tafita@cu.te")
            .reference("STD21001")
            .role(Role.STUDENT)
            .build();
    return JStudent.builder().id(UUID.randomUUID()).user(user).build();
  }
}
