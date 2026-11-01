package com.docsaas.api.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.docsaas.api.enums.FileKind;

class ProcessorsTest {

    @TempDir
    Path tmp;

    // ===== fixtures =====

    private ProcessingContext.InputFile pdf(String name, int pages) throws IOException {
        Path file = tmp.resolve(name);
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }
            doc.save(file.toFile());
        }
        return new ProcessingContext.InputFile(file, name, FileKind.PDF);
    }

    /** A PDF with a large, noisy, losslessly embedded photo: compressible by JPEG re-encoding. */
    private ProcessingContext.InputFile pdfWithImage(String name) throws IOException {
        Path file = tmp.resolve(name);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDImageXObject img = LosslessFactory.createFromImage(doc, photo(1200, 900));
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(img, 20, 20, 500, 375);
            }
            doc.save(file.toFile());
        }
        return new ProcessingContext.InputFile(file, name, FileKind.PDF);
    }

    private static BufferedImage photo(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, Color.ORANGE, w, h, Color.BLUE));
        g.fillRect(0, 0, w, h);
        g.dispose();
        Random r = new Random(42);
        for (int i = 0; i < w * h / 4; i++) {
            img.setRGB(r.nextInt(w), r.nextInt(h), r.nextInt());
        }
        return img;
    }

    private ProcessingContext.InputFile image(String name, String format, int w, int h) throws IOException {
        Path file = tmp.resolve(name);
        ImageIO.write(photo(w, h), format, file.toFile());
        return new ProcessingContext.InputFile(file, name, format.equals("png") ? FileKind.PNG : FileKind.JPEG);
    }

    private ProcessingContext ctx(List<ProcessingContext.InputFile> inputs, Map<String, String> options)
            throws IOException {
        Path work = Files.createDirectories(tmp.resolve("work-" + System.nanoTime()));
        return new ProcessingContext(inputs, options, work);
    }

    private static int pageCount(Path pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            return doc.getNumberOfPages();
        }
    }

    // ===== merge =====

    @Test
    void mergeKeepsAllPagesInOrder() throws IOException {
        var result = new MergePdfProcessor().process(ctx(List.of(pdf("a.pdf", 2), pdf("b.pdf", 3)), Map.of()));
        assertThat(pageCount(result.file())).isEqualTo(5);
        assertThat(result.fileName()).isEqualTo("merged.pdf");
        assertThat(result.summary()).contains("5 pages");
    }

    @Test
    void mergeRejectsCorruptPdf() throws IOException {
        Path bad = tmp.resolve("bad.pdf");
        Files.writeString(bad, "%PDF-1.4 this is not really a pdf");
        var input = new ProcessingContext.InputFile(bad, "bad.pdf", FileKind.PDF);
        assertThatThrownBy(() -> new MergePdfProcessor().process(ctx(List.of(pdf("a.pdf", 1), input), Map.of())))
                .isInstanceOf(ProcessingException.class)
                .hasMessageContaining("bad.pdf");
    }

    @Test
    void rejectsPasswordProtectedPdf() throws IOException {
        Path file = tmp.resolve("locked.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.protect(new StandardProtectionPolicy("owner", "user", new AccessPermission()));
            doc.save(file.toFile());
        }
        var input = new ProcessingContext.InputFile(file, "locked.pdf", FileKind.PDF);
        assertThatThrownBy(() -> new CompressPdfProcessor().process(ctx(List.of(input), Map.of())))
                .isInstanceOf(ProcessingException.class)
                .extracting("code").isEqualTo("PDF_ENCRYPTED");
    }

    // ===== split =====

    @Test
    void splitSingleRangeReturnsPdf() throws IOException {
        var result = new SplitPdfProcessor().process(
                ctx(List.of(pdf("doc.pdf", 10)), Map.of("mode", "ranges", "ranges", "2-4")));
        assertThat(result.mimeType()).isEqualTo("application/pdf");
        assertThat(result.fileName()).isEqualTo("doc_pages_2-4.pdf");
        assertThat(pageCount(result.file())).isEqualTo(3);
    }

    @Test
    void splitMultipleRangesReturnsZip() throws IOException {
        var result = new SplitPdfProcessor().process(
                ctx(List.of(pdf("doc.pdf", 10)), Map.of("mode", "ranges", "ranges", "1-2, 5, 7-10")));
        assertThat(result.mimeType()).isEqualTo("application/zip");
        try (ZipFile zip = new ZipFile(result.file().toFile())) {
            assertThat(zip.size()).isEqualTo(3);
        }
    }

    @Test
    void splitExtractCombinesRanges() throws IOException {
        var result = new SplitPdfProcessor().process(
                ctx(List.of(pdf("doc.pdf", 10)), Map.of("mode", "extract", "ranges", "1, 3-4")));
        assertThat(pageCount(result.file())).isEqualTo(3);
    }

    @Test
    void splitEveryPage() throws IOException {
        var result = new SplitPdfProcessor().process(ctx(List.of(pdf("doc.pdf", 4)), Map.of("mode", "every")));
        try (ZipFile zip = new ZipFile(result.file().toFile())) {
            assertThat(zip.size()).isEqualTo(4);
        }
    }

    /**
     * Regression: pages whose fonts are inherited from the page tree lost their text, and a link
     * to another page pulled the whole source (here: a large image on the last page) into each part.
     */
    @Test
    void splitKeepsInheritedResourcesAndDropsCrossPageLinks() throws IOException {
        Path file = tmp.resolve("linked.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int i = 1; i <= 3; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(helvetica, 18);
                    cs.newLineAtOffset(72, 700);
                    cs.showText("Hello from page " + i);
                    cs.endText();
                }
            }
            PDPage last = doc.getPage(2);
            PDImageXObject img = LosslessFactory.createFromImage(doc, photo(1200, 900));
            last.getResources().add(img);
            // Fonts of pages 1-2 live only on the page tree root, as some PDF generators write them.
            PDResources shared = doc.getPage(0).getResources();
            doc.getPages().getCOSObject().setItem(COSName.RESOURCES, shared);
            for (int i = 0; i < 2; i++) {
                doc.getPage(i).getCOSObject().removeItem(COSName.RESOURCES);
                PDPageFitDestination dest = new PDPageFitDestination();
                dest.setPage(last);
                PDAnnotationLink link = new PDAnnotationLink();
                link.setRectangle(new PDRectangle(72, 690, 200, 20));
                link.setDestination(dest);
                doc.getPage(i).getAnnotations().add(link);
            }
            doc.save(file.toFile());
        }
        var input = new ProcessingContext.InputFile(file, "linked.pdf", FileKind.PDF);

        var result = new SplitPdfProcessor().process(ctx(List.of(input), Map.of("mode", "ranges", "ranges", "1")));

        try (PDDocument part = Loader.loadPDF(result.file().toFile())) {
            assertThat(part.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(part)).contains("Hello from page 1");
        }
        assertThat(Files.size(result.file())).isLessThan(Files.size(file) / 10);
    }

    @Test
    void splitRejectsRangeBeyondDocument() {
        assertThatThrownBy(() -> new SplitPdfProcessor().process(
                ctx(List.of(pdf("doc.pdf", 3)), Map.of("ranges", "2-9"))))
                .isInstanceOf(ProcessingException.class)
                .hasMessageContaining("3 pages");
    }

    @Test
    void pageRangeSyntaxValidation() {
        assertThat(PageRanges.parse(" 1 - 3 ,5,, 7")).hasSize(3);
        assertThatThrownBy(() -> PageRanges.parse("3-1")).isInstanceOf(ProcessingException.class);
        assertThatThrownBy(() -> PageRanges.parse("0")).isInstanceOf(ProcessingException.class);
        assertThatThrownBy(() -> PageRanges.parse("abc")).isInstanceOf(ProcessingException.class);
        assertThatThrownBy(() -> PageRanges.parse("1-2-3")).isInstanceOf(ProcessingException.class);
        assertThatThrownBy(() -> PageRanges.parse("")).isInstanceOf(ProcessingException.class);
    }

    @Test
    void invalidOptionValueIsRejectedUpFront() {
        assertThatThrownBy(() -> new CompressPdfProcessor().validateOptions(Map.of("level", "extreme")))
                .isInstanceOf(ProcessingException.class)
                .extracting("code").isEqualTo("INVALID_OPTION");
    }

    // ===== compress =====

    @Test
    void compressShrinksImageHeavyPdf() throws IOException {
        var input = pdfWithImage("photo.pdf");
        var result = new CompressPdfProcessor().process(ctx(List.of(input), Map.of("level", "medium")));
        assertThat(Files.size(result.file())).isLessThan(Files.size(input.path()));
        assertThat(pageCount(result.file())).isEqualTo(1);
        assertThat(result.summary()).contains("smaller");
    }

    @Test
    void compressNeverReturnsLargerFile() throws IOException {
        var input = pdf("text.pdf", 1);
        var result = new CompressPdfProcessor().process(ctx(List.of(input), Map.of("level", "low")));
        assertThat(Files.size(result.file())).isLessThanOrEqualTo(Files.size(input.path()));
    }

    // ===== conversions =====

    @Test
    void pdfToImagesZipsMultiplePages() throws IOException {
        var result = new PdfToImagesProcessor().process(
                ctx(List.of(pdf("doc.pdf", 3)), Map.of("format", "jpg", "dpi", "72")));
        try (ZipFile zip = new ZipFile(result.file().toFile())) {
            assertThat(zip.size()).isEqualTo(3);
            assertThat(zip.getEntry("doc_page_1.jpg")).isNotNull();
        }
    }

    @Test
    void pdfToImagesSinglePageReturnsImage() throws IOException {
        var result = new PdfToImagesProcessor().process(ctx(List.of(pdf("one.pdf", 1)), Map.of("dpi", "72")));
        assertThat(result.mimeType()).isEqualTo("image/png");
        assertThat(ImageIO.read(result.file().toFile())).isNotNull();
    }

    @Test
    void imagesToPdfOnePagePerImage() throws IOException {
        List<ProcessingContext.InputFile> inputs = new ArrayList<>();
        inputs.add(image("a.png", "png", 400, 300));
        inputs.add(image("b.jpg", "jpg", 300, 400));
        var result = new ImagesToPdfProcessor().process(ctx(inputs, Map.of("pageSize", "a4")));
        try (PDDocument doc = Loader.loadPDF(result.file().toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
            // landscape image on auto orientation -> landscape A4
            assertThat(doc.getPage(0).getMediaBox().getWidth()).isGreaterThan(doc.getPage(0).getMediaBox().getHeight());
        }
    }

    @Test
    void compressImageReducesJpeg() throws IOException {
        var input = image("p.jpg", "jpg", 1600, 1200);
        var result = new CompressImageProcessor().process(
                ctx(List.of(input), Map.of("level", "high", "maxDimension", "1280")));
        assertThat(Files.size(result.file())).isLessThan(Files.size(input.path()));
        assertThat(ImageIO.read(result.file().toFile()).getWidth()).isEqualTo(1280);
    }

    @Test
    void compressImageAlwaysHonoursRequestedResize() throws IOException {
        // Noisy PNG: resizing may not reduce bytes, but the user still gets the requested dimensions.
        var input = image("noise.png", "png", 1600, 1200);
        var result = new CompressImageProcessor().process(
                ctx(List.of(input), Map.of("level", "high", "maxDimension", "1280")));
        assertThat(ImageIO.read(result.file().toFile()).getWidth()).isEqualTo(1280);
        assertThat(result.summary()).startsWith("Resized to 1280×960 px.");
    }

    @Test
    void pngToJpgAndBack() throws IOException {
        var formats = new ImageFormatProcessors();
        var jpg = formats.pngToJpgProcessor().process(ctx(List.of(image("x.png", "png", 50, 40)), Map.of()));
        assertThat(jpg.fileName()).isEqualTo("x.jpg");
        assertThat(FileTypeDetector.detect(Files.newInputStream(jpg.file()))).contains(FileKind.JPEG);

        var jpgInput = new ProcessingContext.InputFile(jpg.file(), "x.jpg", FileKind.JPEG);
        var png = formats.jpgToPngProcessor().process(ctx(List.of(jpgInput), Map.of()));
        assertThat(FileTypeDetector.detect(Files.newInputStream(png.file()))).contains(FileKind.PNG);
    }

    // ===== detection & names =====

    @Test
    void detectsTypesByContentNotExtension() {
        assertThat(FileTypeDetector.detect("%PDF-1.7\n".getBytes())).contains(FileKind.PDF);
        assertThat(FileTypeDetector.detect(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0 })).contains(FileKind.JPEG);
        assertThat(FileTypeDetector.detect("MZ\u0090\u0000 executable".getBytes())).isEmpty();
        assertThat(FileTypeDetector.detect(new byte[0])).isEmpty();
    }

    @Test
    void baseNameStripsPathsAndUnsafeCharacters() {
        assertThat(FileNames.baseName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(FileNames.baseName("C:\\Users\\me\\Report 2026.pdf")).isEqualTo("Report 2026");
        assertThat(FileNames.baseName("a\"b<c>.pdf")).isEqualTo("a_b_c_");
        assertThat(FileNames.baseName(null)).isEqualTo("document");
    }
}
