package com.docsaas.api.processing;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

/**
 * Draws text on top of existing page content in "as displayed" coordinates: (0,0) is the
 * bottom-left corner the reader sees, whatever the page's /Rotate and crop box. Text comes out
 * upright on rotated pages too.
 */
final class PageOverlay implements AutoCloseable {

    static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final PDPageContentStream cs;
    private final PDRectangle box;
    private final int rotation;

    PageOverlay(PDDocument doc, PDPage page) throws IOException {
        // resetContext: the page's own drawing state (transforms, colors) must not leak into ours
        this.cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);
        this.box = page.getCropBox();
        this.rotation = ((page.getRotation() % 360) + 360) % 360;
    }

    /** Width of the page as the reader sees it. */
    float width() {
        return rotation == 90 || rotation == 270 ? box.getHeight() : box.getWidth();
    }

    float height() {
        return rotation == 90 || rotation == 270 ? box.getWidth() : box.getHeight();
    }

    /**
     * Draws text whose anchor point (x, y) is in displayed coordinates. {@code align} 0 = left,
     * 0.5 = centered, 1 = right; {@code middle} puts the anchor at the middle of the letters instead
     * of the baseline; {@code angle} is in degrees, counter-clockwise as seen by the reader.
     */
    void text(String text, PDFont font, float size, float x, float y, float align, boolean middle, float angle,
            float opacity, float gray) throws IOException {
        // displayed point -> user space (inverse of the viewer's clockwise /Rotate)
        float w = box.getWidth();
        float h = box.getHeight();
        float u;
        float v;
        switch (rotation) {
            case 90 -> { u = w - y; v = x; }
            case 180 -> { u = w - x; v = h - y; }
            case 270 -> { u = y; v = h - x; }
            default -> { u = x; v = y; }
        }
        float textWidth = font.getStringWidth(text) / 1000f * size;
        // Cap height is ~0.7em, so its middle is 0.35em above the baseline
        float dy = middle ? -size * 0.35f : 0;

        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(opacity);

        cs.saveGraphicsState();
        cs.setGraphicsStateParameters(gs);
        cs.setNonStrokingColor(gray, gray, gray);
        cs.beginText();
        cs.setFont(font, size);
        Matrix m = Matrix.getRotateInstance(Math.toRadians(angle + rotation), box.getLowerLeftX() + u, box.getLowerLeftY() + v);
        m.translate(-textWidth * align, dy);
        cs.setTextMatrix(m);
        cs.showText(text);
        cs.endText();
        cs.restoreGraphicsState();
    }

    /** Standard fonts only cover Latin text (WinAnsi); reject anything else up front. */
    static void checkEncodable(String text, String what) {
        try {
            BOLD.encode(text);
        } catch (IOException | IllegalArgumentException e) {
            throw new ProcessingException("UNSUPPORTED_TEXT",
                    what + " can only use Latin letters, digits and common symbols for now.");
        }
    }

    @Override
    public void close() throws IOException {
        cs.close();
    }
}
