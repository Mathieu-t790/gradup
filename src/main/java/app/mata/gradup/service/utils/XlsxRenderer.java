package app.mata.gradup.service.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Deterministic, dependency-free XLSX writer.
 *
 * <p>The whole point of this class is byte-for-byte reproducible output: every ZIP entry has a
 * fixed timestamp and none of the embedded XML carries a creation date, so the produced bytes can
 * be compared against a committed golden file in integration tests.
 */
public final class XlsxRenderer {

  private static final String CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private XlsxRenderer() {}

  /**
   * Renders a single-sheet workbook.
   *
   * @param sheetName sheet name (kept short: Excel caps it at 31 chars)
   * @param headers first row
   * @param rows data rows, one list of cells per row
   */
  public static byte[] render(String sheetName, List<String> headers, List<List<String>> rows) {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
      write(zip, "[Content_Types].xml", contentTypes());
      write(zip, "_rels/.rels", rootRels());
      write(zip, "xl/workbook.xml", workbook(sheetName));
      write(zip, "xl/_rels/workbook.xml.rels", workbookRels());
      write(zip, "xl/styles.xml", styles());
      write(zip, "xl/worksheets/sheet1.xml", worksheet(headers, rows));
      zip.finish();
      return buffer.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Could not render xlsx workbook", e);
    }
  }

  public static String contentType() {
    return CONTENT_TYPE;
  }

  private static void write(ZipOutputStream zip, String name, String content) throws IOException {
    ZipEntry entry = new ZipEntry(name);
    entry.setTime(0L);
    zip.putNextEntry(entry);
    zip.write(content.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  private static String contentTypes() {
    return """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>
""";
  }

  private static String rootRels() {
    return """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>
""";
  }

  private static String workbookRels() {
    return """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
""";
  }

  private static String workbook(String sheetName) {
    return """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="%s" sheetId="1" r:id="rId1"/></sheets>
</workbook>
"""
        .formatted(xml(sheetName));
  }

  private static String styles() {
    return """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>
""";
  }

  private static String worksheet(List<String> headers, List<List<String>> rows) {
    StringBuilder sheet =
        new StringBuilder(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
            """);
    sheet.append(row(1, headers, true));
    int rowNumber = 2;
    for (List<String> row : rows) {
      sheet.append(row(rowNumber, row, false));
      rowNumber++;
    }
    sheet.append("</sheetData></worksheet>");
    return sheet.toString();
  }

  private static String row(int rowNumber, List<String> cells, boolean header) {
    StringBuilder row = new StringBuilder("<row r=\"").append(rowNumber).append("\">");
    for (int i = 0; i < cells.size(); i++) {
      String value = cells.get(i) == null ? "" : cells.get(i);
      int column = i + 1;
      row.append("<c r=\"").append(columnName(column)).append(rowNumber).append('"');
      if (header) {
        row.append(" s=\"1\"");
      }
      row.append(" t=\"inlineStr\"><is><t>").append(xml(value)).append("</t></is></c>");
    }
    row.append("</row>");
    return row.toString();
  }

  private static String columnName(int column) {
    StringBuilder name = new StringBuilder();
    while (column > 0) {
      int remainder = (column - 1) % 26;
      name.insert(0, (char) ('A' + remainder));
      column = (column - 1) / 26;
    }
    return name.toString();
  }

  private static String xml(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
