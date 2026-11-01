package com.docsaas.api.processing.engine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

/** Extracts text page by page, using OCR for pages that have no text layer (scans). */
@Service
public class PdfTextExtractor {

    /** Pages with fewer meaningful characters than this are treated as scanned. */
    private static final int MIN_TEXT_CHARS = 25;

    private final OcrEngine ocr;

    public PdfTextExtractor(OcrEngine ocr) {
        this.ocr = ocr;
    }

    public record PageText(int page, String text, boolean ocr) {
    }

    public List<PageText> extract(PDDocument doc, int maxPages) throws IOException {
        int pages = Math.min(doc.getNumberOfPages(), maxPages);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);

        boolean ocrAvailable = ocr.unavailableReason() == null;
        String ocrLanguage = ocrAvailable ? (ocr.installedLanguages().contains("eng") ? "eng" : ocr.installedLanguages().iterator().next()) : null;
        PDFRenderer renderer = null;

        List<PageText> result = new ArrayList<>();
        for (int p = 1; p <= pages; p++) {
            stripper.setStartPage(p);
            stripper.setEndPage(p);
            String text = normalize(stripper.getText(doc));
            if (meaningfulChars(text) < MIN_TEXT_CHARS && ocrAvailable) {
                if (renderer == null) {
                    renderer = new PDFRenderer(doc);
                }
                BufferedImage image = renderer.renderImageWithDPI(p - 1, 300, ImageType.GRAY);
                result.add(new PageText(p, normalize(ocr.imageToText(image, ocrLanguage)), true));
            } else {
                result.add(new PageText(p, text, false));
            }
        }
        return result;
    }

    static String normalize(String text) {
        return text.replace("\r", "")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private static int meaningfulChars(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) {
                n++;
            }
        }
        return n;
    }
}
