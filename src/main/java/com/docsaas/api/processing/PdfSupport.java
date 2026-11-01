package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

public final class PdfSupport {

    public static final int MAX_PAGES = 500;

    private PdfSupport() {
    }

    /** Loads a PDF using temp-file buffering (bounded heap) and rejects unsupported documents with clear messages. */
    public static PDDocument load(ProcessingContext.InputFile input) {
        PDDocument doc = open(input, "");
        if (doc.isEncrypted()) {
            close(doc);
            throw new ProcessingException("PDF_ENCRYPTED",
                    "\"" + input.originalName() + "\" is protected. Remove the protection with Unlock PDF first.");
        }
        return checkPages(doc, input);
    }

    /**
     * Loads a possibly encrypted PDF with the given password (empty for PDFs that only restrict
     * permissions). The caller decides what to do with the encryption.
     */
    public static PDDocument loadWithPassword(ProcessingContext.InputFile input, String password) {
        return checkPages(open(input, password == null ? "" : password), input);
    }

    private static PDDocument open(ProcessingContext.InputFile input, String password) {
        try {
            return Loader.loadPDF(new RandomAccessReadBufferedFile(input.path().toFile()),
                    password, null, null, IOUtils.createTempFileOnlyStreamCache());
        } catch (InvalidPasswordException e) {
            throw new ProcessingException("PDF_ENCRYPTED", password.isEmpty()
                    ? "\"" + input.originalName() + "\" is password protected. Unlock it with Unlock PDF first."
                    : "The password is not correct for \"" + input.originalName() + "\".");
        } catch (IOException e) {
            throw new ProcessingException("INVALID_PDF",
                    "\"" + input.originalName() + "\" could not be read. The file may be damaged.");
        }
    }

    private static PDDocument checkPages(PDDocument doc, ProcessingContext.InputFile input) {
        if (doc.getNumberOfPages() == 0) {
            close(doc);
            throw new ProcessingException("EMPTY_PDF", "\"" + input.originalName() + "\" has no pages.");
        }
        if (doc.getNumberOfPages() > MAX_PAGES) {
            close(doc);
            throw new ProcessingException("TOO_MANY_PAGES",
                    "\"" + input.originalName() + "\" has more than " + MAX_PAGES + " pages, which is the current limit.");
        }
        return doc;
    }

    public static PDDocument newDocument() {
        return new PDDocument(IOUtils.createTempFileOnlyStreamCache());
    }

    public static void save(PDDocument doc, Path target) throws IOException {
        doc.save(target.toFile());
    }

    public static void close(PDDocument doc) {
        try {
            doc.close();
        } catch (IOException ignored) {
            // nothing useful to do
        }
    }
    /**
     * PDDocument.importPage alone is not enough (same fixes as PDFBox's Splitter):
     * - resources inherited from the source page tree are not copied, so text and images go missing;
     * - links and annotations keep references to other source pages, which drags the whole
     *   source document into every part (each part as large as the original).
     */
    public static PDPage importPage(PDDocument target, PDPage page) throws IOException {
        PDPage imported = target.importPage(page);
        if (page.getResources() != null && !page.getCOSObject().containsKey(COSName.RESOURCES)) {
            imported.setResources(page.getResources());
        }
        for (PDAnnotation annotation : imported.getAnnotations()) {
            if (annotation instanceof PDAnnotationLink link) {
                PDDestination destination = link.getDestination();
                if (destination == null && link.getAction() instanceof PDActionGoTo goTo) {
                    destination = goTo.getDestination();
                }
                if (destination instanceof PDPageDestination pageDestination) {
                    pageDestination.setPage(null);
                }
            }
            annotation.setPage(null);
        }
        return imported;
    }

    /**
     * Maps the page "as displayed" (origin bottom-left of what the reader sees, y up, in points) to
     * the page's own coordinates, undoing /Rotate and the crop box offset.
     */
    public static java.awt.geom.AffineTransform displayToUser(PDPage page) {
        org.apache.pdfbox.pdmodel.common.PDRectangle crop = page.getCropBox();
        float w = crop.getWidth();
        float h = crop.getHeight();
        float llx = crop.getLowerLeftX();
        float lly = crop.getLowerLeftY();
        return switch (((page.getRotation() % 360) + 360) % 360) {
            case 90 -> new java.awt.geom.AffineTransform(0, 1, -1, 0, llx + w, lly);
            case 180 -> new java.awt.geom.AffineTransform(-1, 0, 0, -1, llx + w, lly + h);
            case 270 -> new java.awt.geom.AffineTransform(0, -1, 1, 0, llx, lly + h);
            default -> new java.awt.geom.AffineTransform(1, 0, 0, 1, llx, lly);
        };
    }

    /** Displayed width and height of a page (crop box, swapped for 90/270 rotation). */
    public static float[] displaySize(PDPage page) {
        org.apache.pdfbox.pdmodel.common.PDRectangle crop = page.getCropBox();
        int r = ((page.getRotation() % 360) + 360) % 360;
        return r == 90 || r == 270 ? new float[] { crop.getHeight(), crop.getWidth() } : new float[] { crop.getWidth(), crop.getHeight() };
    }
}
