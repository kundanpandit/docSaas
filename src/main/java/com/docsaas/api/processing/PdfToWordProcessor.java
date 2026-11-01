package com.docsaas.api.processing;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.processing.engine.OcrEngine;

/**
 * PDF to editable DOCX (pure Java: PDFBox + Apache POI). Keeps text, paragraphs, headings
 * (by font size), bold text, page breaks and images. Exact layout (columns, tables, text
 * positions) is not reproduced; scanned pages are OCR'd when OCR is available.
 */
@Component
public class PdfToWordProcessor implements ToolProcessor {

    private static final int MAX_IMAGES_PER_PAGE = 20;
    private static final double MAX_IMAGE_WIDTH_PT = 450;

    private final OcrEngine ocr;

    public PdfToWordProcessor(OcrEngine ocr) {
        this.ocr = ocr;
    }

    @Override
    public ToolCode tool() {
        return ToolCode.PDF_TO_WORD;
    }

    /** A paragraph of text with the style signals we keep. */
    record Para(String text, float fontSize, boolean bold) {
    }

    /** Collects paragraphs per page with their largest font size and boldness. */
    static class ParagraphCollector extends PDFTextStripper {
        final List<List<Para>> pages = new ArrayList<>();
        private List<Para> current;
        private final StringBuilder text = new StringBuilder();
        private float maxSize;
        private int boldChars;
        private int chars;

        ParagraphCollector() throws IOException {
            setSortByPosition(true);
            setShouldSeparateByBeads(true);
        }

        @Override
        protected void startPage(PDPage page) throws IOException {
            current = new ArrayList<>();
            pages.add(current);
            super.startPage(page);
        }

        @Override
        protected void endPage(PDPage page) throws IOException {
            flush();
            super.endPage(page);
        }

        @Override
        protected void writeString(String string, List<TextPosition> positions) {
            text.append(string);
            for (TextPosition tp : positions) {
                maxSize = Math.max(maxSize, tp.getFontSizeInPt());
                String font = tp.getFont() == null || tp.getFont().getName() == null ? "" : tp.getFont().getName().toLowerCase();
                if (font.contains("bold") || font.contains("black") || font.contains("heavy")) {
                    boldChars++;
                }
                chars++;
            }
        }

        @Override
        protected void writeLineSeparator() {
            text.append(' ');
        }

        @Override
        protected void writeWordSeparator() {
            text.append(' ');
        }

        @Override
        protected void writeParagraphEnd() {
            flush();
        }

        private void flush() {
            String t = text.toString().replaceAll("\\s+", " ").strip();
            if (!t.isEmpty() && current != null) {
                current.add(new Para(t, maxSize, chars > 0 && boldChars * 2 > chars));
            }
            text.setLength(0);
            maxSize = 0;
            boldChars = 0;
            chars = 0;
        }
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        boolean ocrAvailable = ocr.unavailableReason() == null;
        int ocrPages = 0;
        int pageCount;

        Path out = ctx.workDir().resolve("converted.docx");
        PDDocument pdf = PdfSupport.load(input);
        try (XWPFDocument docx = new XWPFDocument()) {
            ParagraphCollector collector = new ParagraphCollector();
            collector.getText(pdf);
            float bodySize = bodyFontSize(collector.pages);
            pageCount = pdf.getNumberOfPages();
            PDFRenderer renderer = null;

            for (int p = 0; p < pdf.getNumberOfPages(); p++) {
                List<Para> paras = p < collector.pages.size() ? collector.pages.get(p) : List.of();

                if (paras.isEmpty() && ocrAvailable) {
                    // No text layer: probably a scan. OCR it so the user still gets editable text.
                    if (renderer == null) {
                        renderer = new PDFRenderer(pdf);
                    }
                    BufferedImage image = renderer.renderImageWithDPI(p, 300, ImageType.GRAY);
                    String lang = ocr.installedLanguages().contains("eng") ? "eng" : ocr.installedLanguages().iterator().next();
                    paras = new ArrayList<>();
                    for (String block : ocr.imageToText(image, lang).split("\\n\\s*\\n")) {
                        String t = block.replaceAll("\\s+", " ").strip();
                        if (!t.isEmpty()) {
                            paras.add(new Para(t, bodySize, false));
                        }
                    }
                    ocrPages++;
                } else {
                    addImages(docx, pdf.getPage(p));
                }

                for (Para para : paras) {
                    writeParagraph(docx, para, bodySize);
                }

                if (p < pdf.getNumberOfPages() - 1) {
                    docx.createParagraph().createRun().addBreak(BreakType.PAGE);
                }
            }

            try (OutputStream os = Files.newOutputStream(out)) {
                docx.write(os);
            }
        } finally {
            PdfSupport.close(pdf);
        }

        String summary = "Converted " + pageCount + (pageCount == 1 ? " page" : " pages")
                + " to an editable Word document. Complex layouts such as tables and columns may need small touch-ups.";
        if (ocrPages > 0) {
            summary += " " + ocrPages + (ocrPages == 1 ? " scanned page was" : " scanned pages were") + " read with OCR.";
        }
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + ".docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", summary);
    }

    private static void writeParagraph(XWPFDocument docx, Para para, float bodySize) {
        XWPFParagraph paragraph = docx.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(para.text());
        run.setFontFamily("Calibri");

        boolean heading = bodySize > 0 && para.fontSize() >= bodySize * 1.3f && para.text().length() <= 150;
        if (heading) {
            run.setBold(true);
            run.setFontSize(para.fontSize() >= bodySize * 1.8f ? 20 : 15);
            paragraph.setSpacingBefore(240);
        } else {
            run.setBold(para.bold());
            run.setFontSize(11);
        }
        paragraph.setSpacingAfter(120);
    }

    /**
     * Body text size = the font size covering the most characters. (A median over paragraphs
     * fails when headings are as numerous as body paragraphs.)
     */
    private static float bodyFontSize(List<List<Para>> pages) {
        java.util.Map<Integer, Integer> charsBySize = new java.util.HashMap<>();
        pages.forEach(page -> page.forEach(p -> {
            if (p.fontSize() > 0) {
                charsBySize.merge(Math.round(p.fontSize()), p.text().length(), Integer::sum);
            }
        }));
        return charsBySize.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(e -> (float) e.getKey())
                .orElse(11f);
    }

    private static void addImages(XWPFDocument docx, PDPage page) {
        PDResources resources = page.getResources();
        if (resources == null) {
            return;
        }
        int added = 0;
        for (COSName name : resources.getXObjectNames()) {
            if (added >= MAX_IMAGES_PER_PAGE) {
                break;
            }
            try {
                PDXObject xobject = resources.getXObject(name);
                if (!(xobject instanceof PDImageXObject image) || image.getWidth() < 48 || image.getHeight() < 48
                        || (long) image.getWidth() * image.getHeight() > ImageSupport.MAX_PIXELS) {
                    continue;
                }
                BufferedImage bi = image.getImage();
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(bi, "png", png);

                double width = Math.min(MAX_IMAGE_WIDTH_PT, bi.getWidth() * 0.75);
                double height = width * bi.getHeight() / bi.getWidth();
                XWPFRun run = docx.createParagraph().createRun();
                run.addPicture(new ByteArrayInputStream(png.toByteArray()), Document.PICTURE_TYPE_PNG,
                        "image" + added + ".png", Units.toEMU(width), Units.toEMU(height));
                added++;
            } catch (Exception e) {
                // unsupported image encoding: skip it, keep converting the rest
            }
        }
    }
}
