package com.docsaas.api.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import com.docsaas.api.enums.FileKind;
import com.fasterxml.jackson.databind.ObjectMapper;

class EditPdfProcessorTest {

    @TempDir
    Path tmp;

    private final ObjectMapper mapper = new ObjectMapper();

    private EditPdfProcessor processor() {
        EditorFonts fonts = new EditorFonts();
        ReflectionTestUtils.setField(fonts, "unicodeFonts",
                "C:/Windows/Fonts/Nirmala.ttc,/usr/share/fonts/truetype/noto/NotoSansDevanagari-Regular.ttf");
        return new EditPdfProcessor(fonts, mapper);
    }

    /** A4 pages each saying "Original page N" at (72, 100) from the top-left. */
    private ProcessingContext.InputFile pdf(int pages, int rotate) throws IOException {
        Path file = tmp.resolve("doc.pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 1; i <= pages; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                page.setRotation(rotate);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 24);
                    cs.newLineAtOffset(72, 842 - 100);
                    cs.showText("Original page " + i);
                    cs.endText();
                }
            }
            doc.save(file.toFile());
        }
        return new ProcessingContext.InputFile(file, "doc.pdf", FileKind.PDF);
    }

    private ProcessingResult edit(List<ProcessingContext.InputFile> inputs, String json) throws IOException {
        Path data = tmp.resolve("data-" + System.nanoTime() + ".json");
        Files.writeString(data, json.replace('\'', '"'));
        processor().validateData(mapper.readTree(data.toFile()));
        Path work = Files.createDirectories(tmp.resolve("work-" + System.nanoTime()));
        return processor().process(new ProcessingContext(inputs, Map.of(), work, data));
    }

    private static List<String> pageTexts(Path pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            List<String> texts = new ArrayList<>();
            PDFTextStripper s = new PDFTextStripper();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                s.setStartPage(p);
                s.setEndPage(p);
                texts.add(s.getText(doc).strip());
            }
            return texts;
        }
    }

    /** Displayed position (x, baseline y from the top) and direction of the first glyph of a word. */
    private static float[] glyph(Path pdf, int page, String word) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            float[][] found = new float[1][];
            PDFTextStripper s = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    int at = text.indexOf(word);
                    if (found[0] == null && at >= 0) {
                        TextPosition t = positions.get(at);
                        found[0] = new float[] { t.getX(), t.getY(), t.getDir() };
                    }
                }
            };
            s.setSortByPosition(true);
            s.setStartPage(page);
            s.setEndPage(page);
            s.getText(doc);
            return found[0];
        }
    }

    private static Color pixel(Path pdf, int page, float x, float y) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            BufferedImage img = new PDFRenderer(doc).renderImageWithDPI(page, 72);
            return new Color(img.getRGB(Math.round(x), Math.round(y)));
        }
    }

    /** Number of dark pixels in a region (72 dpi, so pixels = points). */
    private static int darkPixels(Path pdf, int page, int x0, int y0, int x1, int y1) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            BufferedImage img = new PDFRenderer(doc).renderImageWithDPI(page, 72);
            int n = 0;
            for (int x = x0; x < x1; x++) {
                for (int y = y0; y < y1; y++) {
                    if (new Color(img.getRGB(x, y)).getRed() < 128) {
                        n++;
                    }
                }
            }
            return n;
        }
    }

    // ===== pages =====

    @Test
    void reordersDeletesDuplicatesAndInsertsBlankPages() throws IOException {
        var result = edit(List.of(pdf(3, 0)), """
                {'pages': [{'source': 3}, {'blank': true, 'width': 612, 'height': 792}, {'source': 1}, {'source': 1, 'rotate': 90}],
                 'objects': []}""");
        // whitespace ignored: extraction breaks lines differently on the rotated page
        assertThat(pageTexts(result.file()).stream().map(t -> t.replaceAll("\\s", "")).toList())
                .containsExactly("Originalpage3", "", "Originalpage1", "Originalpage1");
        try (PDDocument doc = Loader.loadPDF(result.file().toFile())) {
            assertThat(doc.getPage(1).getMediaBox().getWidth()).isEqualTo(612);
            assertThat(doc.getPage(3).getRotation()).isEqualTo(90);
            assertThat(doc.getPage(2).getRotation()).isZero();
        }
        assertThat(result.fileName()).isEqualTo("doc_edited.pdf");
    }

    // ===== text =====

    @Test
    void textLandsWhereItWasPlaced() throws IOException {
        var result = edit(List.of(pdf(1, 0)), """
                {'pages': [{'source': 1}],
                 'objects': [{'page': 0, 'type': 'text', 'x': 100, 'y': 300, 'text': 'Added line\\nSecond line', 'size': 20, 'color': '#cc0000'}]}""");
        assertThat(pageTexts(result.file()).get(0)).contains("Original page 1", "Added line", "Second line");
        float[] first = glyph(result.file(), 1, "Added");
        float[] second = glyph(result.file(), 1, "Second");
        assertThat(first[0]).isCloseTo(100f, org.assertj.core.data.Offset.offset(1f));
        // baseline = top + 0.9465 em (Arial metrics at line-height 1.2), next line 1.2 em lower
        assertThat(first[1]).isCloseTo(300 + 0.9465f * 20, org.assertj.core.data.Offset.offset(1.5f));
        assertThat(second[1] - first[1]).isCloseTo(24f, org.assertj.core.data.Offset.offset(0.5f));
    }

    @Test
    void textOnRotatedPageUsesTheDisplayedCoordinates() throws IOException {
        // /Rotate 90: displayed page is landscape 842 x 595
        var result = edit(List.of(pdf(1, 90)), """
                {'pages': [{'source': 1}],
                 'objects': [{'page': 0, 'type': 'text', 'x': 600, 'y': 80, 'text': 'Upright', 'size': 18}]}""");
        float[] g = glyph(result.file(), 1, "Upright");
        assertThat(g[0]).isCloseTo(600f, org.assertj.core.data.Offset.offset(1.5f));
        assertThat(g[1]).isCloseTo(80 + 0.9465f * 18, org.assertj.core.data.Offset.offset(1.5f));
        assertThat(g[2]).isEqualTo(90f); // upright for the reader (direction is measured in unrotated space)
    }

    @Test
    void objectsTurnWithThePageWhenTheUserRotatesIt() throws IOException {
        var result = edit(List.of(pdf(1, 0)), """
                {'pages': [{'source': 1, 'rotate': 90}],
                 'objects': [{'page': 0, 'type': 'text', 'x': 100, 'y': 300, 'text': 'Attached', 'size': 18},
                             {'page': 0, 'type': 'text', 'x': 300, 'y': 500, 'text': 'Typed upright', 'size': 18, 'angle': -90}]}""");
        // like the original text, the first label turns with the page...
        float[] original = glyph(result.file(), 1, "Original");
        float[] attached = glyph(result.file(), 1, "Attached");
        assertThat(attached[2]).isEqualTo(original[2]);
        // ...while text typed on the turned page (angle -90) is upright for the reader
        float[] upright = glyph(result.file(), 1, "Typed");
        assertThat(upright[2]).isNotEqualTo(original[2]);
    }

    @Test
    void hindiTextUsesAnEmbeddedUnicodeFont() throws IOException {
        assumeTrue(Files.exists(Path.of("C:/Windows/Fonts/Nirmala.ttc"))
                || Files.exists(Path.of("/usr/share/fonts/truetype/noto/NotoSansDevanagari-Regular.ttf")),
                "no Unicode font installed");
        var result = edit(List.of(pdf(1, 0)), """
                {'pages': [{'source': 1}],
                 'objects': [{'page': 0, 'type': 'text', 'x': 72, 'y': 400, 'text': 'नमस्ते दुनिया', 'size': 24, 'bold': true}]}""");
        assertThat(pageTexts(result.file()).get(0)).contains("नमस्ते");
    }

    // ===== shapes and images =====

    @Test
    void whiteoutCoversAndHighlightTints() throws IOException {
        var input = pdf(1, 0);
        // the original text sits in x 72..250, y 80..100 (baseline 100, 24 pt)
        assertThat(darkPixels(input.path(), 0, 72, 80, 250, 102)).isGreaterThan(100);
        var result = edit(List.of(input), """
                {'pages': [{'source': 1}],
                 'objects': [{'page': 0, 'type': 'whiteout', 'x': 70, 'y': 75, 'w': 250, 'h': 32},
                             {'page': 0, 'type': 'highlight', 'x': 70, 'y': 400, 'w': 100, 'h': 20},
                             {'page': 0, 'type': 'rect', 'x': 300, 'y': 400, 'w': 100, 'h': 50, 'stroke': '#0000ff', 'strokeWidth': 4},
                             {'page': 0, 'type': 'arrow', 'x1': 50, 'y1': 600, 'x2': 250, 'y2': 600, 'color': '#00aa00', 'width': 3},
                             {'page': 0, 'type': 'ink', 'points': [[400, 600], [450, 650], [500, 600]], 'color': '#000000', 'width': 3}]}""");
        assertThat(darkPixels(result.file(), 0, 72, 80, 250, 102)).isZero();
        Color highlight = pixel(result.file(), 0, 120, 410);
        assertThat(highlight.getBlue()).isLessThan(200);
        assertThat(highlight.getRed()).isGreaterThan(200);
        assertThat(pixel(result.file(), 0, 302, 425).getBlue()).isGreaterThan(200); // rect outline
        assertThat(pixel(result.file(), 0, 302, 425).getRed()).isLessThan(100);
        assertThat(pixel(result.file(), 0, 150, 600).getGreen()).isGreaterThan(120); // arrow shaft
        assertThat(pixel(result.file(), 0, 450, 650).getRed()).isLessThan(100); // ink
        // the text under the whiteout is removed from the file, not just covered
        assertThat(pageTexts(result.file()).get(0)).doesNotContain("Original");
    }

    @Test
    void placesUploadedImages() throws IOException {
        Path png = tmp.resolve("sig.png");
        BufferedImage img = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 40; x++) {
            for (int y = 0; y < 20; y++) {
                img.setRGB(x, y, 0xff0000);
            }
        }
        ImageIO.write(img, "png", png.toFile());
        var image = new ProcessingContext.InputFile(png, "sig.png", FileKind.PNG);

        var result = edit(List.of(pdf(1, 0), image), """
                {'pages': [{'source': 1}],
                 'objects': [{'page': 0, 'type': 'image', 'image': 1, 'x': 200, 'y': 500, 'w': 120, 'h': 60}]}""");
        Color inside = pixel(result.file(), 0, 260, 530);
        assertThat(inside.getRed()).isGreaterThan(200);
        assertThat(inside.getGreen()).isLessThan(60);
        assertThat(pixel(result.file(), 0, 260, 570)).isEqualTo(Color.WHITE); // below the image
    }

    // ===== erasing text =====

    @Test
    void erasingOneWordKeepsTheRestOfTheLineInPlace() throws IOException {
        var input = pdf(1, 0); // "Original page 1" at x 72, baseline 100, 24 pt
        float[] before = glyph(input.path(), 1, "page");
        // cover only "Original" (72 .. ~158)
        var result = edit(List.of(input), """
                {'pages': [{'source': 1}],
                 'objects': [{'page': 0, 'type': 'whiteout', 'x': 70, 'y': 76, 'w': 88, 'h': 30}]}""");
        String text = pageTexts(result.file()).get(0);
        assertThat(text).doesNotContain("Original").contains("page 1");
        float[] after = glyph(result.file(), 1, "page");
        assertThat(after[0]).isCloseTo(before[0], org.assertj.core.data.Offset.offset(0.5f));
        assertThat(after[1]).isCloseTo(before[1], org.assertj.core.data.Offset.offset(0.5f));
    }

    @Test
    void replacedTextIsTheOnlyVersionInTheFile() throws IOException {
        // what "edit existing text" sends: a whiteout over the run plus the new text at the same baseline
        var result = edit(List.of(pdf(1, 0)), """
                {'pages': [{'source': 1}],
                 'objects': [{'page': 0, 'type': 'whiteout', 'x': 71, 'y': 76, 'w': 200, 'h': 30},
                             {'page': 0, 'type': 'text', 'x': 72, 'y': 77.3, 'text': 'Edited page 1', 'size': 24}]}""");
        assertThat(pageTexts(result.file()).get(0)).isEqualTo("Edited page 1");
    }

    @Test
    void whiteoutCanKeepTheTextUnderneath() throws IOException {
        var result = edit(List.of(pdf(1, 0)), """
                {'pages': [{'source': 1}],
                 'objects': [{'page': 0, 'type': 'whiteout', 'x': 70, 'y': 76, 'w': 250, 'h': 30, 'keepText': true}]}""");
        assertThat(pageTexts(result.file()).get(0)).contains("Original page 1");
    }

    @Test
    void erasesTextOnRotatedPages() throws IOException {
        // /Rotate 90: displayed landscape; the text sits at displayed (742, 72) .. rotated with the page
        var input = pdf(1, 90);
        float[] g = glyph(input.path(), 1, "Original");
        var result = edit(List.of(input), String.format(java.util.Locale.ROOT, """
                {'pages': [{'source': 1}],
                 'objects': [{'page': 0, 'type': 'whiteout', 'x': %.1f, 'y': %.1f, 'w': 40, 'h': 220}]}""",
                g[0] - 25, g[1] - 10));
        assertThat(pageTexts(result.file()).get(0).replaceAll("\s", "")).doesNotContain("Original");
    }

    // ===== validation =====

    @Test
    void rejectsInvalidPlans() {
        assertThatThrownBy(() -> processor().validateData(null)).hasMessageContaining("Nothing to save");
        assertThatThrownBy(() -> processor().validateData(mapper.readTree("{\"pages\": []}")))
                .hasMessageContaining("at least one page");
        assertThatThrownBy(() -> processor().validateData(mapper.readTree(
                "{\"pages\": [{\"source\": 1}], \"objects\": [{\"page\": 0, \"type\": \"script\"}]}")))
                .hasMessageContaining("Unknown kind");
        assertThatThrownBy(() -> processor().validateData(mapper.readTree(
                "{\"pages\": [{\"source\": 1}], \"objects\": [{\"page\": 3, \"type\": \"rect\", \"x\": 1, \"y\": 1, \"w\": 1, \"h\": 1}]}")))
                .hasMessageContaining("page that is not");
        assertThatThrownBy(() -> processor().validateData(mapper.readTree(
                "{\"pages\": [{\"source\": 1}], \"objects\": [{\"page\": 0, \"type\": \"rect\", \"x\": 1, \"y\": 1, \"w\": 1, \"h\": 1, \"color\": \"red;\"}]}")))
                .hasMessageContaining("Invalid color");
        assertThatThrownBy(() -> processor().validateData(mapper.readTree(
                "{\"pages\": [{\"source\": 1, \"rotate\": 45}]}")))
                .hasMessageContaining("90 degrees");
    }

    @Test
    void rejectsPagesBeyondTheDocument() throws IOException {
        var input = pdf(2, 0);
        assertThatThrownBy(() -> edit(List.of(input), "{'pages': [{'source': 5}]}"))
                .isInstanceOf(ProcessingException.class).hasMessageContaining("not in the document");
    }
}
