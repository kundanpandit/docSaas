package com.docsaas.api.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.docsaas.api.enums.FileKind;

class PdfEditToolsTest {

    @TempDir
    Path tmp;

    // ===== fixtures =====

    private ProcessingContext.InputFile pdf(String name, int pages) throws IOException {
        Path file = tmp.resolve(name);
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(72, 700);
                    cs.showText("Body text on page " + (i + 1));
                    cs.endText();
                }
            }
            doc.save(file.toFile());
        }
        return new ProcessingContext.InputFile(file, name, FileKind.PDF);
    }

    /** A page drawn as rows of cells at fixed x positions, the way invoices and reports lay out tables. */
    private ProcessingContext.InputFile tablePdf(String name, String[][] rows, float[] columnX, boolean rightAlignLast)
            throws IOException {
        Path file = tmp.resolve(name);
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 11);
                cs.newLineAtOffset(72, 780);
                cs.showText("Quarterly sales report");
                cs.endText();
                for (int r = 0; r < rows.length; r++) {
                    for (int c = 0; c < rows[r].length; c++) {
                        float x = columnX[c];
                        if (rightAlignLast && c == rows[r].length - 1) {
                            x = columnX[c] + 80 - font.getStringWidth(rows[r][c]) / 1000f * 11;
                        }
                        cs.beginText();
                        cs.setFont(font, 11);
                        cs.newLineAtOffset(x, 740 - r * 20);
                        cs.showText(rows[r][c]);
                        cs.endText();
                    }
                }
            }
            doc.save(file.toFile());
        }
        return new ProcessingContext.InputFile(file, name, FileKind.PDF);
    }

    private ProcessingContext ctx(ProcessingContext.InputFile input, Map<String, String> options) throws IOException {
        return new ProcessingContext(List.of(input), options, Files.createDirectories(tmp.resolve("work-" + System.nanoTime())));
    }

    private static String text(Path pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    // ===== protect / unlock =====

    @Test
    void protectRequiresThePasswordToOpen() throws IOException {
        var result = new ProtectPdfProcessor().process(ctx(pdf("doc.pdf", 2), Map.of("password", "open sesame")));
        assertThatThrownBy(() -> Loader.loadPDF(result.file().toFile())).isInstanceOf(InvalidPasswordException.class);
        try (PDDocument doc = Loader.loadPDF(result.file().toFile(), "open sesame")) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
            assertThat(doc.getEncryption().getLength()).isEqualTo(256);
        }
        assertThat(new ProtectPdfProcessor().sensitiveOptions()).containsExactly("password");
    }

    @Test
    void protectRejectsShortPassword() {
        assertThatThrownBy(() -> new ProtectPdfProcessor().validateOptions(Map.of("password", "abc")))
                .isInstanceOf(ProcessingException.class).hasMessageContaining("4 to 64");
    }

    @Test
    void unlockWithPasswordRemovesEncryption() throws IOException {
        var locked = new ProtectPdfProcessor().process(ctx(pdf("doc.pdf", 1), Map.of("password", "secret1")));
        var input = new ProcessingContext.InputFile(locked.file(), "doc_protected.pdf", FileKind.PDF);

        var result = new UnlockPdfProcessor().process(ctx(input, Map.of("password", "secret1")));
        try (PDDocument doc = Loader.loadPDF(result.file().toFile())) {
            assertThat(doc.isEncrypted()).isFalse();
        }
        assertThat(result.fileName()).isEqualTo("doc_protected_unlocked.pdf");
    }

    @Test
    void unlockExplainsMissingOrWrongPassword() throws IOException {
        var locked = new ProtectPdfProcessor().process(ctx(pdf("doc.pdf", 1), Map.of("password", "secret1")));
        var input = new ProcessingContext.InputFile(locked.file(), "doc.pdf", FileKind.PDF);

        assertThatThrownBy(() -> new UnlockPdfProcessor().process(ctx(input, Map.of())))
                .isInstanceOf(ProcessingException.class).extracting("code").isEqualTo("PASSWORD_REQUIRED");
        assertThatThrownBy(() -> new UnlockPdfProcessor().process(ctx(input, Map.of("password", "wrong"))))
                .isInstanceOf(ProcessingException.class).hasMessageContaining("not correct");
    }

    @Test
    void unlockRemovesPermissionOnlyRestrictionsWithoutPassword() throws IOException {
        Path file = tmp.resolve("restricted.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            AccessPermission noPrint = new AccessPermission();
            noPrint.setCanPrint(false);
            doc.protect(new StandardProtectionPolicy("owner-only", "", noPrint));
            doc.save(file.toFile());
        }
        var input = new ProcessingContext.InputFile(file, "restricted.pdf", FileKind.PDF);
        var result = new UnlockPdfProcessor().process(ctx(input, Map.of()));
        try (PDDocument doc = Loader.loadPDF(result.file().toFile())) {
            assertThat(doc.isEncrypted()).isFalse();
        }
    }

    @Test
    void unlockRejectsUnprotectedPdf() {
        assertThatThrownBy(() -> new UnlockPdfProcessor().process(ctx(pdf("doc.pdf", 1), Map.of())))
                .isInstanceOf(ProcessingException.class).extracting("code").isEqualTo("NOT_ENCRYPTED");
    }

    @Test
    void otherToolsPointToUnlockForProtectedPdfs() throws IOException {
        var locked = new ProtectPdfProcessor().process(ctx(pdf("doc.pdf", 1), Map.of("password", "secret1")));
        var input = new ProcessingContext.InputFile(locked.file(), "doc.pdf", FileKind.PDF);
        assertThatThrownBy(() -> new RotatePdfProcessor().process(ctx(input, Map.of())))
                .isInstanceOf(ProcessingException.class).hasMessageContaining("Unlock PDF");
    }

    // ===== rotate =====

    @Test
    void rotateSelectedPagesOnly() throws IOException {
        var result = new RotatePdfProcessor().process(
                ctx(pdf("doc.pdf", 4), Map.of("angle", "90", "pages", "ranges", "ranges", "2-3")));
        try (PDDocument doc = Loader.loadPDF(result.file().toFile())) {
            assertThat(doc.getPage(0).getRotation()).isZero();
            assertThat(doc.getPage(1).getRotation()).isEqualTo(90);
            assertThat(doc.getPage(2).getRotation()).isEqualTo(90);
            assertThat(doc.getPage(3).getRotation()).isZero();
        }
        assertThat(result.summary()).contains("2 pages");
    }

    @Test
    void rotateAddsToExistingRotation() throws IOException {
        var once = new RotatePdfProcessor().process(ctx(pdf("doc.pdf", 1), Map.of("angle", "270")));
        var input = new ProcessingContext.InputFile(once.file(), "doc.pdf", FileKind.PDF);
        var twice = new RotatePdfProcessor().process(ctx(input, Map.of("angle", "180")));
        try (PDDocument doc = Loader.loadPDF(twice.file().toFile())) {
            assertThat(doc.getPage(0).getRotation()).isEqualTo(90);
        }
    }

    // ===== watermark / page numbers =====

    @Test
    void watermarkIsAddedToEveryPageAndKeepsContent() throws IOException {
        var result = new WatermarkPdfProcessor().process(
                ctx(pdf("doc.pdf", 3), Map.of("text", "CONFIDENTIAL", "layout", "diagonal", "opacity", "medium")));
        // Glyphs in drawing order: text extraction would split diagonal text into one letter per line
        StringBuilder drawn = new StringBuilder();
        try (PDDocument doc = Loader.loadPDF(result.file().toFile())) {
            new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition t) {
                    drawn.append(t.getUnicode());
                }
            }.getText(doc);
        }
        assertThat(drawn.toString().split("CONFIDENTIAL", -1)).hasSize(4);
        assertThat(text(result.file())).contains("Body text on page 3");
    }

    @Test
    void watermarkRejectsTextTheFontCannotDraw() {
        assertThatThrownBy(() -> new WatermarkPdfProcessor().validateOptions(Map.of("text", "गोपनीय")))
                .isInstanceOf(ProcessingException.class).hasMessageContaining("Latin");
        assertThatThrownBy(() -> new WatermarkPdfProcessor().validateOptions(Map.of("text", " ")))
                .isInstanceOf(ProcessingException.class);
    }

    @Test
    void pageNumbersUseFormatAndStart() throws IOException {
        var result = new PageNumbersProcessor().process(
                ctx(pdf("doc.pdf", 3), Map.of("format", "of-total", "start", "5", "position", "bottom-right")));
        String all = text(result.file());
        assertThat(all).contains("5 of 7").contains("6 of 7").contains("7 of 7");
        assertThat(result.summary()).contains("5-7");
    }

    @Test
    void pageNumbersStayUprightAndInsideRotatedPages() throws IOException {
        var rotated = new RotatePdfProcessor().process(ctx(pdf("doc.pdf", 1), Map.of("angle", "90")));
        var input = new ProcessingContext.InputFile(rotated.file(), "doc.pdf", FileKind.PDF);
        var result = new PageNumbersProcessor().process(ctx(input, Map.of("position", "bottom-center")));

        try (PDDocument doc = Loader.loadPDF(result.file().toFile())) {
            // getX/getY are adjusted for the page rotation (origin top-left as displayed)
            float[] found = new float[3];
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String s, List<TextPosition> positions) {
                    if (s.strip().equals("1")) {
                        TextPosition t = positions.get(0);
                        found[0] = t.getX();
                        found[1] = t.getY();
                        found[2] = t.getDir();
                    }
                }
            };
            stripper.getText(doc);
            // Displayed page is landscape (842 x 595): the number sits near the bottom, horizontally centered
            assertThat(found[0]).isBetween(380f, 460f);
            assertThat(found[1]).isGreaterThan(540f);
            // getDir is measured in unrotated page space: on a /Rotate 90 page, upright text has direction 90
            assertThat(found[2]).isEqualTo(90f);
        }
    }

    @Test
    void pageNumbersRejectBadStart() {
        assertThatThrownBy(() -> new PageNumbersProcessor().validateOptions(Map.of("start", "0")))
                .isInstanceOf(ProcessingException.class);
    }

    // ===== PDF to Excel =====

    @Test
    void pdfToExcelRebuildsTableWithNumericCells() throws IOException {
        String[][] rows = {
                { "Region", "Product", "Revenue" },
                { "North", "Widgets", "1,250.50" },
                { "South", "Gadgets and parts", "980" },
                { "East", "Widgets", "12,000" } };
        var input = tablePdf("sales.pdf", rows, new float[] { 72, 200, 380 }, true);

        var result = new PdfToExcelProcessor().process(ctx(input, Map.of()));
        assertThat(result.fileName()).isEqualTo("sales.xlsx");

        try (InputStream in = Files.newInputStream(result.file()); XSSFWorkbook book = new XSSFWorkbook(in)) {
            Sheet sheet = book.getSheet("Page 1");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Quarterly sales report");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Revenue");
            assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("Gadgets and parts");
            // right-aligned numbers of different widths land in one column, as numbers
            assertThat(sheet.getRow(2).getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(sheet.getRow(2).getCell(2).getNumericCellValue()).isEqualTo(1250.5);
            assertThat(sheet.getRow(3).getCell(2).getNumericCellValue()).isEqualTo(980);
            assertThat(sheet.getRow(4).getCell(2).getNumericCellValue()).isEqualTo(12000);
            assertThat(sheet.getRow(4).getLastCellNum()).isEqualTo((short) 3);
        }
    }

    @Test
    void numberParsingKeepsCodesAsText() {
        assertThat(PdfToExcelProcessor.parseNumber("1,250.50")).isEqualTo(1250.5);
        assertThat(PdfToExcelProcessor.parseNumber("(12)")).isEqualTo(-12.0);
        assertThat(PdfToExcelProcessor.parseNumber("-3.5")).isEqualTo(-3.5);
        assertThat(PdfToExcelProcessor.parseNumber("0.75")).isEqualTo(0.75);
        assertThat(PdfToExcelProcessor.parseNumber("007")).isNull();
        assertThat(PdfToExcelProcessor.parseNumber("1234567890123456")).isNull();
        assertThat(PdfToExcelProcessor.parseNumber("12%")).isNull();
        assertThat(PdfToExcelProcessor.parseNumber("(12")).isNull();
    }

    @Test
    void pdfToExcelRejectsPdfWithoutText() throws IOException {
        Path file = tmp.resolve("blank.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(file.toFile());
        }
        var input = new ProcessingContext.InputFile(file, "blank.pdf", FileKind.PDF);
        assertThatThrownBy(() -> new PdfToExcelProcessor().process(ctx(input, Map.of())))
                .isInstanceOf(ProcessingException.class).hasMessageContaining("Scanned PDF OCR");
    }
}
