package app.mata.gradup.service.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Component
@AllArgsConstructor
public class PdfRenderer {

  private static final String NOTE_FORMAT_PATTERN = "0.0#";
  private static final DateTimeFormatter FRENCH_DATE =
      DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
  private static final String LOGO_DATA_URI = EmailAssets.LOGO_DATA_URI;
  private static final String SIGNATURE_DATA_URI =
      ClasspathImages.dataUri("static/images/signature.png");

  private final HtmlTemplater htmlTemplater;

  public File render(TranscriptPdfData data) {
    try {
      Context context = new Context();
      context.setVariable("data", data);
      context.setVariable("logoDataUri", LOGO_DATA_URI);
      context.setVariable("signatureDataUri", SIGNATURE_DATA_URI);
      context.setVariable("issueDateText", FRENCH_DATE.format(LocalDate.now()));

      String html = htmlTemplater.render("pdf/transcript", context);

      File pdf =
          File.createTempFile(
              Wording.get("transcript.pdf.temp.prefix") + data.student().reference() + "-",
              ".pdf");
      try (OutputStream os = new FileOutputStream(pdf)) {
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(os);
      }
      return pdf;
    } catch (Exception e) {
      throw new RuntimeException("Failed to render transcript PDF", e);
    }
  }

  public record TranscriptPdfData(
      String title,
      StudentInfo student,
      List<CourseLine> courses,
      AbsencesInfo absences,
      ResultInfo result,
      boolean provisional) {

    public record StudentInfo(
        String lastName, String firstName, String reference, String inscriptionLine) {}

    public record CourseLine(String code, String title, int credits, BigDecimal note) {
      public String noteText() {
        return note == null ? null : formatNote(note);
      }
    }

    public record AbsencesInfo(String countText, String justificationText, String malusText) {}

    public record ResultInfo(int creditsAcquired, int totalCredits, BigDecimal weightedAverage) {
      public String weightedAverageText() {
        return weightedAverage == null ? null : formatNote(weightedAverage);
      }
    }
  }

  private static String formatNote(BigDecimal value) {
    return new DecimalFormat(NOTE_FORMAT_PATTERN).format(value);
  }
}
