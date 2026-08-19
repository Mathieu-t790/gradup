package app.mata.gradup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DiplomaExportFilenameTest {

  private static final LocalDate DAY = LocalDate.of(2026, 8, 19);

  @Test
  void el_track_uses_el_label_and_download_date() {
    assertEquals(
        "diplômés_El_Mpamakilay_20260819.xlsx",
        DiplomaService.buildExportFilename(app.mata.gradup.endpoint.rest.model.TrackCode.EL, "Mpamakilay", DAY));
  }

  @Test
  void tn_track_uses_tn_label_and_download_date() {
    assertEquals(
        "diplômés_Tn_Mpamakilay_20260819.xlsx",
        DiplomaService.buildExportFilename(app.mata.gradup.endpoint.rest.model.TrackCode.TN, "Mpamakilay", DAY));
  }

  @Test
  void promotion_export_uses_tronc_commun_scope() {
    assertEquals(
        "diplômés_TroncCommun_Mpamakilay_20260819.xlsx",
        DiplomaService.buildExportFilename(null, "Mpamakilay", DAY));
  }

  @Test
  void single_digit_date_components_are_zero_padded() {
    assertEquals(
        "diplômés_El_Mpamakilay_20260102.xlsx",
        DiplomaService.buildExportFilename(
            app.mata.gradup.endpoint.rest.model.TrackCode.EL, "Mpamakilay", LocalDate.of(2026, 1, 2)));
  }
}