package com.docsaas.api.processing;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.processing.engine.OcrEngine;

/**
 * Scanned PDF to searchable PDF. Pages that already have a text layer are left untouched. Each
 * scanned page is rendered and OCR'd, and only Tesseract's invisible text layer is laid over the
 * original page: the scan itself is never re-encoded, so quality and file size stay the same.
 * Options: language = eng | hin | eng+hin ...
 */
@Component
public class ScannedPdfOcrProcessor implements ToolProcessor {

    /** Pages with at least this many letters/digits of real text are not scans. */
    static final int MIN_TEXT_CHARS = 25;

    private final OcrEngine ocr;

    @Value("${docsaas.ocr.dpi:200}")
    private int dpi;

    @Value("${docsaas.ocr.max-pages:50}")
    private int maxPages;

    public ScannedPdfOcrProcessor(OcrEngine ocr) {
        this.ocr = ocr;
    }

    @Override
    public ToolCode tool() {
        return ToolCode.SCANNED_PDF_OCR;
    }

    @Override
    public String unavailableReason() {
        return ocr.unavailableReason();
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ocr.resolveLanguage(options);
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        String language = ocr.resolveLanguage(ctx.options());
        Path out = ctx.workDir().resolve("searchable.pdf");

        // Text-layer documents must stay open until the target is saved (their forms are imported by reference)
        List<PDDocument> layers = new ArrayList<>();
        int scanned = 0;
        int pages;
        PDDocument doc = PdfSupport.load(input);
        try {
            pages = doc.getNumberOfPages();
            boolean[] needsOcr = new boolean[pages];
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 0; i < pages; i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                needsOcr[i] = meaningfulChars(stripper.getText(doc)) < MIN_TEXT_CHARS;
                if (needsOcr[i]) {
                    scanned++;
                }
            }
            if (scanned == 0) {
                throw new ProcessingException("ALREADY_SEARCHABLE",
                        "This PDF already has selectable text on every page, so it doesn't need OCR. "
                                + "To edit or copy the text, use PDF to Word.");
            }
            if (scanned > maxPages) {
                throw new ProcessingException("TOO_MANY_PAGES",
                        "OCR supports up to " + maxPages + " scanned pages per file. Split the document first.");
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            LayerUtility layerUtility = new LayerUtility(doc);
            // One page at a time keeps memory flat regardless of document length.
            for (int i = 0; i < pages; i++) {
                if (!needsOcr[i]) {
                    continue;
                }
                BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.GRAY);
                // Sideways or upside-down scans are read turned upright; the layer is turned back below
                int turn = ocr.uprightTurn(image, language);
                Path png = OcrEngine.writePageImage(OcrEngine.rotate(image, turn), ctx.workDir().resolve("page_" + i + ".png"));
                Path textPdf = ocr.pageTextLayer(png, language, dpi, ctx.workDir(), i);
                Files.deleteIfExists(png);

                PDDocument layer = Loader.loadPDF(textPdf.toFile());
                layers.add(layer);
                PDFormXObject form = layerUtility.importPageAsForm(layer, 0);
                PDPage page = doc.getPage(i);
                layerUtility.wrapInSaveRestore(page);
                layerUtility.appendFormAsLayer(page, form, toPageSpace(page, turn, form.getBBox()), "OCR text " + (i + 1));
            }
            PdfSupport.save(doc, out);
        } finally {
            PdfSupport.close(doc);
            layers.forEach(PdfSupport::close);
        }

        String name = FileNames.baseName(input.originalName()) + "_searchable.pdf";
        String kept = scanned < pages
                ? " " + (pages - scanned) + (pages - scanned == 1 ? " page already had" : " pages already had")
                        + " text and " + (pages - scanned == 1 ? "was" : "were") + " kept as is."
                : "";
        return new ProcessingResult(out, name, "application/pdf",
                scanned + (scanned == 1 ? " scanned page" : " scanned pages")
                        + " made searchable. You can now select and search the text." + kept);
    }

    /**
     * Maps the text layer onto the page's own coordinates. The layer is laid out like the image
     * Tesseract read: the page as displayed, turned {@code turn} degrees clockwise to make the text
     * upright. Undoes, in order: that turn, then the page's /Rotate and crop box offset.
     */
    static AffineTransform toPageSpace(PDPage page, int turn, PDRectangle layerBox) {
        float[] displayed = PdfSupport.displaySize(page);
        float dw = displayed[0];
        float dh = displayed[1];
        boolean turnedSideways = turn == 90 || turn == 270;
        // Rendering rounds to whole pixels: scale the layer to exactly the (turned) displayed size
        double sx = (turnedSideways ? dh : dw) / layerBox.getWidth();
        double sy = (turnedSideways ? dw : dh) / layerBox.getHeight();

        AffineTransform rotate = PdfSupport.displayToUser(page);
        // upright layer -> displayed page (inverse of the clockwise turn), y pointing up
        AffineTransform untilt = switch (turn) {
            case 90 -> new AffineTransform(0, 1, -1, 0, dw, 0);
            case 180 -> new AffineTransform(-1, 0, 0, -1, dw, dh);
            case 270 -> new AffineTransform(0, -1, 1, 0, 0, dh);
            default -> new AffineTransform();
        };
        rotate.concatenate(untilt);
        rotate.scale(sx, sy);
        rotate.translate(-layerBox.getLowerLeftX(), -layerBox.getLowerLeftY());
        return rotate;
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
