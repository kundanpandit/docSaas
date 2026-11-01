package com.docsaas.api.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import com.docsaas.api.enums.FileKind;
import com.docsaas.api.processing.engine.AiClient;
import com.docsaas.api.processing.engine.DocumentIndex;
import com.docsaas.api.processing.engine.LibreOfficeEngine;
import com.docsaas.api.processing.engine.OcrEngine;
import com.docsaas.api.processing.engine.PdfTextExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;

class NewToolsTest {

    @TempDir
    Path tmp;

    @BeforeAll
    static void plugins() {
        ImageIO.scanForPlugins();
    }

    private static OcrEngine ocr() {
        OcrEngine engine = new OcrEngine();
        ReflectionTestUtils.setField(engine, "tessdataDir", "./tessdata");
        return engine;
    }

    /** Fake AI provider: records prompts and returns a canned answer. */
    static class FakeAi extends AiClient {
        final List<List<Message>> calls = new ArrayList<>();
        String reply = "Overview of the document (p. 1).\n\n## Key points\n- Revenue grew 12% (p. 2)";

        @Override
        public String unavailableReason() {
            return null;
        }

        @Override
        public String complete(List<Message> messages, int maxTokens) {
            calls.add(messages);
            return reply;
        }
    }

    // ===== fixtures =====

    private BufferedImage textImage(String... lines) {
        BufferedImage img = new BufferedImage(1400, 140 + lines.length * 90, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.PLAIN, 56));
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 60, 120 + i * 90);
        }
        g.dispose();
        return img;
    }

    private ProcessingContext.InputFile saveImage(BufferedImage img, String name, String format, FileKind kind) throws IOException {
        Path file = tmp.resolve(name);
        ImageIO.write(img, format, file.toFile());
        return new ProcessingContext.InputFile(file, name, kind);
    }

    /** A PDF with real text: a big heading and body paragraphs on two pages. */
    private ProcessingContext.InputFile textPdf(String name) throws IOException {
        Path file = tmp.resolve(name);
        try (PDDocument doc = new PDDocument()) {
            String[][] pages = {
                    { "Annual Report", "The company had a strong year with new customers in three regions." },
                    { "Finance", "Revenue grew 12 percent while operating costs stayed flat across the year." } };
            for (String[] page : pages) {
                PDPage p = new PDPage(PDRectangle.A4);
                doc.addPage(p);
                try (PDPageContentStream cs = new PDPageContentStream(doc, p)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 26);
                    cs.newLineAtOffset(72, 740);
                    cs.showText(page[0]);
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                    cs.newLineAtOffset(0, -40);
                    cs.showText(page[1]);
                    cs.endText();
                }
            }
            doc.save(file.toFile());
        }
        return new ProcessingContext.InputFile(file, name, FileKind.PDF);
    }

    /** An image-only PDF, like a scanner produces: no text layer at all. */
    private ProcessingContext.InputFile scannedPdf(String name) throws IOException {
        Path file = tmp.resolve(name);
        try (PDDocument doc = new PDDocument()) {
            PDPage p = new PDPage(PDRectangle.A4);
            doc.addPage(p);
            var image = LosslessFactory.createFromImage(doc, textImage("INVOICE NUMBER 4471", "Total amount due"));
            try (PDPageContentStream cs = new PDPageContentStream(doc, p)) {
                cs.drawImage(image, 40, 500, 515, 515f * image.getHeight() / image.getWidth());
            }
            doc.save(file.toFile());
        }
        return new ProcessingContext.InputFile(file, name, FileKind.PDF);
    }

    private ProcessingContext ctx(List<ProcessingContext.InputFile> inputs, Map<String, String> options) throws IOException {
        return new ProcessingContext(inputs, options, Files.createDirectories(tmp.resolve("work-" + System.nanoTime())));
    }

    private static String pdfText(Path pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    // ===== OCR =====

    @Test
    void ocrIsAvailableWithInstalledLanguages() {
        assertThat(ocr().installedLanguages()).contains("eng", "hin");
        assertThat(ocr().unavailableReason()).isNull();
    }

    @Test
    void imageToTextReadsPrintedText() throws IOException {
        var input = saveImage(textImage("Hello DocSaaS 2026", "OCR works offline"), "scan.png", "png", FileKind.PNG);
        var result = new ImageToTextProcessor(ocr()).process(ctx(List.of(input), Map.of("language", "eng")));
        String text = Files.readString(result.file());
        assertThat(text).containsIgnoringCase("DocSaaS").contains("2026").containsIgnoringCase("offline");
        assertThat(result.fileName()).isEqualTo("scan.txt");
    }

    @Test
    void ocrRejectsUnknownLanguage() {
        assertThatThrownBy(() -> new ImageToTextProcessor(ocr()).validateOptions(Map.of("language", "xyz")))
                .isInstanceOf(ProcessingException.class).hasMessageContaining("not available");
    }

    @Test
    void scannedPdfBecomesSearchable() throws IOException {
        var input = scannedPdf("scan.pdf");
        assertThat(pdfText(input.path()).strip()).isEmpty();

        var processor = new ScannedPdfOcrProcessor(ocr());
        ReflectionTestUtils.setField(processor, "dpi", 200);
        ReflectionTestUtils.setField(processor, "maxPages", 50);
        var result = processor.process(ctx(List.of(input), Map.of()));

        assertThat(pdfText(result.file())).containsIgnoringCase("invoice").contains("4471");
        try (PDDocument doc = Loader.loadPDF(result.file().toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    private ScannedPdfOcrProcessor ocrProcessor() {
        var processor = new ScannedPdfOcrProcessor(ocr());
        ReflectionTestUtils.setField(processor, "dpi", 200);
        ReflectionTestUtils.setField(processor, "maxPages", 50);
        return processor;
    }

    /** Regression: a digital PDF was rasterized page by page (458 KB became 11 MB of OCR'd images). */
    @Test
    void ocrRefusesPdfThatAlreadyHasText() {
        assertThatThrownBy(() -> ocrProcessor().process(ctx(List.of(textPdf("report.pdf")), Map.of())))
                .isInstanceOf(ProcessingException.class)
                .extracting("code").isEqualTo("ALREADY_SEARCHABLE");
    }

    @Test
    void ocrKeepsTextPagesAndOnlyAddsInvisibleTextToScans() throws IOException {
        // page 1: real text; page 2: scan
        Path file = tmp.resolve("mixed.pdf");
        try (PDDocument text = Loader.loadPDF(textPdf("t.pdf").path().toFile());
                PDDocument scan = Loader.loadPDF(scannedPdf("s.pdf").path().toFile());
                PDDocument mixed = new PDDocument()) {
            mixed.importPage(text.getPage(0));
            mixed.importPage(scan.getPage(0));
            mixed.save(file.toFile());
        }
        var input = new ProcessingContext.InputFile(file, "mixed.pdf", FileKind.PDF);

        var result = ocrProcessor().process(ctx(List.of(input), Map.of()));

        try (PDDocument out = Loader.loadPDF(result.file().toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            // the real text is untouched (no OCR noise)
            assertThat(stripper.getText(out)).contains("The company had a strong year with new customers in three regions.");
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertThat(stripper.getText(out)).containsIgnoringCase("invoice").contains("4471");
            // the scan keeps its single original image: nothing was re-rendered into the page
            var names = new ArrayList<org.apache.pdfbox.cos.COSName>();
            out.getPage(1).getResources().getXObjectNames().forEach(names::add);
            long images = names.stream().filter(n -> {
                try {
                    return out.getPage(1).getResources().getXObject(n) instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
                } catch (IOException e) {
                    return false;
                }
            }).count();
            assertThat(images).isEqualTo(1);
        }
        assertThat(Files.size(result.file())).isLessThan(Files.size(file) + 60_000);
        assertThat(result.summary()).contains("1 scanned page").contains("1 page already had text");
    }

    /**
     * A one-page scan whose picture is stored turned by {@code storedTurn} degrees clockwise,
     * drawn at x=300..418, y=40..555 on a portrait page with the given /Rotate.
     */
    private ProcessingContext.InputFile turnedScan(String name, int storedTurn, int pageRotate) throws IOException {
        Path file = tmp.resolve(name);
        try (PDDocument doc = new PDDocument()) {
            PDPage p = new PDPage(PDRectangle.A4);
            p.setRotation(pageRotate);
            doc.addPage(p);
            var image = LosslessFactory.createFromImage(doc,
                    OcrEngine.rotate(textImage("INVOICE NUMBER 4471", "Total amount due"), storedTurn));
            try (PDPageContentStream cs = new PDPageContentStream(doc, p)) {
                cs.drawImage(image, 300, 40, 118, 515);
            }
            doc.save(file.toFile());
        }
        return new ProcessingContext.InputFile(file, name, FileKind.PDF);
    }

    /** First position (as displayed: x from left, y from top) of a word in the PDF, or null. */
    private static float[] positionOf(Path pdf, String word) throws IOException {
        try (PDDocument out = Loader.loadPDF(pdf.toFile())) {
            float[][] pos = new float[1][];
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String s, List<org.apache.pdfbox.text.TextPosition> positions) {
                    int at = s.toUpperCase().indexOf(word);
                    if (pos[0] == null && at >= 0) {
                        pos[0] = new float[] { positions.get(at).getX(), positions.get(at).getY() };
                    }
                }
            };
            // sorted by position: vertical text (sideways scans) is grouped into words too
            stripper.setSortByPosition(true);
            stripper.getText(out);
            return pos[0];
        }
    }

    @Test
    void ocrTextLayerLinesUpOnPagesDisplayedWithRotate() throws IOException {
        // Picture stored turned left; /Rotate 90 shows it upright on a landscape page:
        // as displayed it spans x 40..555 and y (from top) 300..418.
        var result = ocrProcessor().process(ctx(List.of(turnedScan("rotated.pdf", 270, 90)), Map.of()));
        float[] pos = positionOf(result.file(), "INVOICE");
        assertThat(pos).as("INVOICE found").isNotNull();
        assertThat(pos[0]).as("x as displayed").isBetween(40f, 200f);
        assertThat(pos[1]).as("y as displayed").isBetween(290f, 430f);
    }

    @Test
    void ocrReadsSidewaysScansAndLinesUpTheText() throws IOException {
        // No /Rotate: the reader sees the picture turned right (sideways), as phone scans often are.
        // As displayed it spans x 300..418, y (from top) 287..802; the first line is at the right edge.
        var result = ocrProcessor().process(ctx(List.of(turnedScan("sideways.pdf", 90, 0)), Map.of()));
        float[] pos = positionOf(result.file(), "INVOICE");
        assertThat(pos).as("INVOICE found").isNotNull();
        assertThat(pos[0]).as("x as displayed").isBetween(330f, 420f);
        assertThat(pos[1]).as("y as displayed").isBetween(280f, 450f);
    }

    @Test
    void imageToTextReadsUpsideDownPhoto() throws IOException {
        var input = saveImage(OcrEngine.rotate(textImage("Hello DocSaaS 2026", "OCR works offline"), 180),
                "upside.png", "png", FileKind.PNG);
        var result = new ImageToTextProcessor(ocr()).process(ctx(List.of(input), Map.of("language", "eng")));
        assertThat(Files.readString(result.file())).containsIgnoringCase("DocSaaS").contains("2026");
    }

    // ===== PDF to Word =====

    @Test
    void pdfToWordKeepsHeadingsAndParagraphs() throws IOException {
        var result = new PdfToWordProcessor(ocr()).process(ctx(List.of(textPdf("report.pdf")), Map.of()));
        assertThat(result.fileName()).isEqualTo("report.docx");

        try (InputStream in = Files.newInputStream(result.file()); XWPFDocument docx = new XWPFDocument(in)) {
            List<XWPFParagraph> paras = docx.getParagraphs().stream().filter(p -> !p.getText().isBlank()).toList();
            assertThat(paras).extracting(XWPFParagraph::getText)
                    .contains("Annual Report", "Finance")
                    .anyMatch(t -> t.contains("Revenue grew 12 percent"));
            XWPFParagraph heading = paras.stream().filter(p -> p.getText().equals("Annual Report")).findFirst().orElseThrow();
            assertThat(heading.getRuns().get(0).isBold()).isTrue();
            assertThat(heading.getRuns().get(0).getFontSizeAsDouble()).isGreaterThan(11.0);
        }
    }

    @Test
    void pdfToWordReadsScannedPagesWithOcr() throws IOException {
        var result = new PdfToWordProcessor(ocr()).process(ctx(List.of(scannedPdf("scan.pdf")), Map.of()));
        try (InputStream in = Files.newInputStream(result.file()); XWPFDocument docx = new XWPFDocument(in)) {
            String all = String.join(" ", docx.getParagraphs().stream().map(XWPFParagraph::getText).toList());
            assertThat(all).containsIgnoringCase("invoice");
        }
        assertThat(result.summary()).contains("OCR");
    }

    // ===== WEBP =====

    @Test
    void convertsPngToWebpAndBack() throws IOException {
        var png = saveImage(textImage("WEBP test"), "pic.png", "png", FileKind.PNG);
        var toWebp = new WebpConverterProcessor().process(ctx(List.of(png), Map.of("format", "webp", "quality", "high")));
        assertThat(FileTypeDetector.detect(Files.readAllBytes(toWebp.file()))).contains(FileKind.WEBP);
        assertThat(toWebp.fileName()).isEqualTo("pic.webp");

        var webp = new ProcessingContext.InputFile(toWebp.file(), "pic.webp", FileKind.WEBP);
        var back = new WebpConverterProcessor().process(ctx(List.of(webp), Map.of("format", "jpg")));
        assertThat(FileTypeDetector.detect(Files.readAllBytes(back.file()))).contains(FileKind.JPEG);
        assertThat(ImageIO.read(back.file().toFile()).getWidth()).isEqualTo(1400);
    }

    @Test
    void webpToWebpIsRejected() throws IOException {
        var png = saveImage(textImage("x"), "a.png", "png", FileKind.PNG);
        var webp = new WebpConverterProcessor().process(ctx(List.of(png), Map.of("format", "webp")));
        var input = new ProcessingContext.InputFile(webp.file(), "a.webp", FileKind.WEBP);
        assertThatThrownBy(() -> new WebpConverterProcessor().process(ctx(List.of(input), Map.of("format", "webp"))))
                .isInstanceOf(ProcessingException.class).hasMessageContaining("already WEBP");
    }

    // ===== AI =====

    @Test
    void aiSummaryUsesDocumentTextWithPageMarkers() throws IOException {
        FakeAi ai = new FakeAi();
        var processor = new AiSummaryProcessor(ai, new PdfTextExtractor(ocr()));
        ReflectionTestUtils.setField(processor, "maxPages", 100);
        ReflectionTestUtils.setField(processor, "maxInputChars", 120_000);

        var result = processor.process(ctx(List.of(textPdf("report.pdf")), Map.of("length", "short")));

        assertThat(ai.calls).hasSize(1);
        String prompt = ai.calls.get(0).get(1).content();
        assertThat(prompt).contains("[Page 1]").contains("[Page 2]").contains("Revenue grew 12 percent");
        assertThat(Files.readString(result.file())).contains("Key points").contains("AI-generated summary");
        assertThat(result.fileName()).isEqualTo("report_summary.md");
    }

    @Test
    void aiSummaryMapReducesLongDocuments() throws IOException {
        FakeAi ai = new FakeAi();
        var processor = new AiSummaryProcessor(ai, new PdfTextExtractor(ocr()));
        ReflectionTestUtils.setField(processor, "maxPages", 100);
        ReflectionTestUtils.setField(processor, "maxInputChars", 100); // force the long-document path

        processor.process(ctx(List.of(textPdf("report.pdf")), Map.of()));
        // at least one "part" call plus the final combine call
        assertThat(ai.calls.size()).isGreaterThanOrEqualTo(2);
        assertThat(ai.calls.get(ai.calls.size() - 1).get(1).content()).contains("Combine them");
    }

    @Test
    void insightsVerifyQuotesAgainstThePageText() throws IOException {
        FakeAi ai = new FakeAi();
        ai.reply = """
                Here you go:
                ```json
                {"overview": "An annual report.", "documentType": "report", "topics": ["Finance", "finance", "Growth"],
                 "keyPoints": [
                   {"text": "Revenue grew 12%", "page": 1, "quote": "Revenue grew 12 percent while operating costs"},
                   {"text": "Profit doubled", "page": 2, "quote": "profit doubled compared to last year"},
                   {"text": "Revenue grew 12%", "page": 2, "quote": "Revenue grew 12 percent"}],
                 "entities": [{"name": "Acme", "type": "company", "role": "issuer", "page": 99, "quote": "Acme Ltd"}],
                 "dates": [], "figures": [], "actionItems": "none"}
                ```""";
        ObjectMapper mapper = new ObjectMapper();
        var processor = new DocumentInsightsProcessor(ai, new PdfTextExtractor(ocr()), mapper);
        ReflectionTestUtils.setField(processor, "maxPages", 100);
        ReflectionTestUtils.setField(processor, "maxInputChars", 120_000);

        var result = processor.process(ctx(List.of(textPdf("report.pdf")), Map.of()));
        var json = mapper.readTree(result.file().toFile());

        assertThat(result.fileName()).isEqualTo("report_insights.json");
        assertThat(json.path("topics")).hasSize(2);
        var points = json.path("keyPoints");
        assertThat(points).hasSize(2); // duplicate dropped
        // quoted text is on page 2, not the cited page 1: page corrected and marked verified
        assertThat(points.get(0).path("verified").asBoolean()).isTrue();
        assertThat(points.get(0).path("page").asInt()).isEqualTo(2);
        // invented claim: kept but flagged
        assertThat(points.get(1).path("verified").asBoolean()).isFalse();
        var acme = json.path("entities").get(0);
        assertThat(acme.path("type").asText()).isEqualTo("other");
        assertThat(acme.path("page").isNull()).isTrue();
        assertThat(json.path("actionItems")).isEmpty();
        assertThat(result.summary()).contains("3 insights").contains("1 verified");
    }

    @Test
    void insightsRetryOnUnreadableAiAnswer() throws IOException {
        FakeAi ai = new FakeAi();
        ai.reply = "Sorry, I cannot do that.";
        var processor = new DocumentInsightsProcessor(ai, new PdfTextExtractor(ocr()), new ObjectMapper());
        ReflectionTestUtils.setField(processor, "maxPages", 100);
        ReflectionTestUtils.setField(processor, "maxInputChars", 120_000);
        var context = ctx(List.of(textPdf("report.pdf")), Map.of());

        assertThatThrownBy(() -> processor.process(context))
                .isInstanceOf(AiClient.AiException.class)
                .extracting("retryable").isEqualTo(true);
    }

    @Test
    void chatIndexHasPageTaggedChunksAndSearchFindsTheRightPage() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        var processor = new ChatWithPdfProcessor(new FakeAi(), new PdfTextExtractor(ocr()), mapper);
        ReflectionTestUtils.setField(processor, "maxPages", 100);

        var result = processor.process(ctx(List.of(textPdf("report.pdf")), Map.of()));
        DocumentIndex index = mapper.readValue(result.file().toFile(), DocumentIndex.class);

        assertThat(index.pages()).isEqualTo(2);
        List<DocumentIndex.Chunk> hits = index.search("How much did revenue grow?", 300);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).page()).isEqualTo(2);
    }

    @Test
    void chatCitationsOnlyKeepPagesThatWereProvided() {
        var excerpts = List.of(new DocumentIndex.Chunk(2, "Revenue grew"), new DocumentIndex.Chunk(5, "Costs"));
        assertThat(com.docsaas.api.service.ChatServiceTestAccess.citedPages(
                "Revenue grew 12% (p. 2). Also see page 5 and p. 9.", excerpts)).containsExactly(2, 5);
    }

    // ===== Word to PDF / detection =====

    @Test
    void detectsWebpAndWordDocuments() throws IOException {
        ByteArrayOutputStream docx = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(docx)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.putNextEntry(new ZipEntry("word/document.xml"));
        }
        assertThat(FileTypeDetector.detect(docx.toByteArray())).contains(FileKind.DOCX);
        assertThat(FileTypeDetector.isWordDocument(new java.io.ByteArrayInputStream(docx.toByteArray()))).isTrue();

        ByteArrayOutputStream otherZip = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(otherZip)) {
            zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
        }
        assertThat(FileTypeDetector.isWordDocument(new java.io.ByteArrayInputStream(otherZip.toByteArray()))).isFalse();
        assertThat(FileTypeDetector.detect(new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' })).contains(FileKind.WEBP);
    }

    @Test
    void wordToPdfWhenLibreOfficeIsInstalled() throws IOException {
        LibreOfficeEngine engine = new LibreOfficeEngine();
        ReflectionTestUtils.setField(engine, "configuredPath", "");
        ReflectionTestUtils.setField(engine, "timeoutSeconds", 120L);
        assumeTrue(engine.unavailableReason() == null, "LibreOffice not installed - skipping");

        Path docx = tmp.resolve("letter.docx");
        try (XWPFDocument doc = new XWPFDocument(); var os = Files.newOutputStream(docx)) {
            doc.createParagraph().createRun().setText("Hello from Word");
            doc.write(os);
        }
        var input = new ProcessingContext.InputFile(docx, "letter.docx", FileKind.DOCX);
        var result = new WordToPdfProcessor(engine).process(ctx(List.of(input), Map.of()));
        assertThat(pdfText(result.file())).contains("Hello from Word");
    }
}
