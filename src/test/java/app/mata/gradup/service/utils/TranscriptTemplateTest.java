package app.mata.gradup.service.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.AbsencesInfo;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.CourseLine;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.ResultInfo;
import app.mata.gradup.service.utils.PdfRenderer.TranscriptPdfData.StudentInfo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;

class TranscriptTemplateTest {

  private final HtmlTemplater templater = new HtmlTemplater();

  @Test
  void pdf_provisional_contains_the_temporary_result_warning() {
    String html = renderPdf(true);
    assertTrue(
        html.contains(
            "NB: Ce résultat affiché est temporaire et ne reflète pas le résultat final."),
        "provisional transcripts must carry the temporary-result warning");
  }

  @Test
  void pdf_final_or_diploma_hides_the_temporary_result_warning() {
    String html = renderPdf(false);
    assertFalse(
        html.contains("Ce résultat affiché est temporaire"),
        "final/diploma transcripts must not carry the temporary-result warning");
  }

  @Test
  void pdf_diploma_uses_the_diploma_title() {
    String html = renderPdf(false, "Diplôme de Licence");
    assertTrue(html.contains("Diplôme de Licence"));
  }

  @Test
  void email_template_contains_the_download_button_with_the_presigned_url() {
    Context context = new Context();
    context.setVariable("studentName", "Tafita Mathieu");
    context.setVariable(
        "downloadUrl", "https://bucket.example/transcript.pdf?X-Amz-Expires=259200");
    context.setVariable("signatureDataUri", EmailAssets.SIGNATURE_DATA_URI);

    String html = templater.render("email/transcript", context);

    assertTrue(html.contains("Télécharger le relevé de notes"));
    assertTrue(html.contains("https://bucket.example/transcript.pdf?X-Amz-Expires=259200"));
    assertTrue(html.contains("Ce lien est valable 3 jours"));
    assertFalse(html.contains("pièce jointe"));
  }

  private static String renderPdf(boolean provisional) {
    return renderPdf(provisional, "Relevé de notes L1");
  }

  private static String renderPdf(boolean provisional, String title) {
    Context context = new Context();
    context.setVariable(
        "data",
        new TranscriptPdfData(
            title,
            new StudentInfo(
                "Mathieu", "Tafita", "STD21001", "Inscrit(e) en Première année de Licence EL"),
            List.of(new CourseLine("PROG1", "Algorithmique", 6, new BigDecimal("12.50"))),
            new AbsencesInfo(null, null, null),
            new ResultInfo(6, 6, new BigDecimal("12.50")),
            provisional));
    context.setVariable("logoDataUri", EmailAssets.LOGO_DATA_URI);
    context.setVariable("signatureDataUri", EmailAssets.SIGNATURE_DATA_URI);
    context.setVariable("issueDateText", "19 août 2026");
    return new HtmlTemplater().render("pdf/transcript", context);
  }
}
