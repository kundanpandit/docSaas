package com.docsaas.api.processing;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.util.Matrix;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Draws editor objects on one page. Coordinates are the page as displayed, in points, origin at
 * the top-left corner and y pointing down: the same space the browser editor uses (pdf.js viewport
 * at scale 1). Object fields:
 *
 * <ul>
 * <li>text: x, y (top-left of the first line), text (lines separated by \n), size, font
 * (helvetica|times|courier), bold, italic, color, angle (0/90/180/270, clockwise about x,y)</li>
 * <li>whiteout, highlight, rect: x, y, w, h, color (fill), stroke, strokeWidth, opacity</li>
 * <li>line, arrow: x1, y1, x2, y2, color, width</li>
 * <li>ink: points [[x, y], ...], color, width, opacity</li>
 * <li>image: x, y, w, h, image (index of the uploaded file), angle, opacity</li>
 * </ul>
 */
final class EditCanvas implements AutoCloseable {

    /** Line height used by the editor (CSS line-height). */
    static final float LINE_HEIGHT = 1.2f;

    /**
     * Distance from the top of a line box to the baseline, per point of font size, for the browser
     * fonts the editor shows (Arial, Times New Roman, Courier New, line-height 1.2): (1.2 - (ascent +
     * descent)) / 2 + ascent. Keeps saved text exactly where it was typed.
     */
    static final Map<String, Float> BASELINE = Map.of("helvetica", 0.9465f, "times", 0.9375f, "courier", 0.8665f);

    private static final double MAX_COORD = 20_000;

    private final PDPageContentStream cs;
    private final EditorFonts.ForDocument fonts;

    EditCanvas(PDDocument doc, PDPage page, EditorFonts.ForDocument fonts) throws IOException {
        this.fonts = fonts;
        // resetContext: the page's own drawing state must not leak into ours
        this.cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);
        float height = PdfSupport.displaySize(page)[1];
        AffineTransform toUser = PdfSupport.displayToUser(page);
        toUser.concatenate(new AffineTransform(1, 0, 0, -1, 0, height)); // y down, origin top-left
        cs.transform(new Matrix(toUser));
    }

    // ===== validation =====

    /** Rejects malformed objects before any work is done. */
    static void check(JsonNode o) {
        String type = o.path("type").asText();
        List<String> numbers = switch (type) {
            case "text" -> List.of("x", "y", "size");
            case "line", "arrow" -> List.of("x1", "y1", "x2", "y2");
            case "ink" -> List.of();
            default -> List.of("x", "y", "w", "h");
        };
        for (String field : numbers) {
            JsonNode n = o.path(field);
            if (!n.isNumber() || !Double.isFinite(n.asDouble()) || Math.abs(n.asDouble()) > MAX_COORD) {
                throw EditPdfProcessor.invalid("A " + type + " change has an invalid \"" + field + "\".");
            }
        }
        switch (type) {
            case "text" -> {
                String text = o.path("text").asText("");
                double size = o.path("size").asDouble();
                if (text.length() > 5000) {
                    throw EditPdfProcessor.invalid("A text box is too long (maximum 5000 characters).");
                }
                if (size < 2 || size > 500) {
                    throw EditPdfProcessor.invalid("Text size must be between 2 and 500.");
                }
            }
            case "ink" -> {
                JsonNode points = o.path("points");
                if (!points.isArray() || points.size() < 1 || points.size() > 20_000) {
                    throw EditPdfProcessor.invalid("A drawing could not be read.");
                }
                for (JsonNode p : points) {
                    if (!p.isArray() || p.size() != 2 || !p.get(0).isNumber() || !p.get(1).isNumber()
                            || Math.abs(p.get(0).asDouble()) > MAX_COORD || Math.abs(p.get(1).asDouble()) > MAX_COORD) {
                        throw EditPdfProcessor.invalid("A drawing could not be read.");
                    }
                }
            }
            case "whiteout", "highlight", "rect", "image" -> {
                if (o.path("w").asDouble() <= 0 || o.path("h").asDouble() <= 0) {
                    throw EditPdfProcessor.invalid("A " + type + " has no size.");
                }
            }
            default -> {
            }
        }
        color(o, "color", null);
        color(o, "stroke", null);
        int angle = o.path("angle").asInt(0);
        if (angle % 90 != 0) {
            throw EditPdfProcessor.invalid("Objects can only be turned in steps of 90 degrees.");
        }
    }

    // ===== drawing =====

    void draw(JsonNode o, PDImageXObject image) throws IOException {
        cs.saveGraphicsState();
        try {
            switch (o.path("type").asText()) {
                case "text" -> text(o);
                case "whiteout" -> box(o, color(o, "color", Color.WHITE), null, 0, 1f, false);
                case "highlight" -> box(o, color(o, "color", new Color(255, 230, 0)), null, 0, opacity(o, 0.4f), true);
                case "rect" -> box(o, color(o, "color", null), color(o, "stroke", Color.BLACK),
                        (float) o.path("strokeWidth").asDouble(2), opacity(o, 1f), false);
                case "line" -> line(o, false);
                case "arrow" -> line(o, true);
                case "ink" -> ink(o);
                case "image" -> image(o, image);
                default -> throw EditPdfProcessor.invalid("Unknown kind of change.");
            }
        } finally {
            cs.restoreGraphicsState();
        }
    }

    private void text(JsonNode o) throws IOException {
        String family = switch (o.path("font").asText("helvetica")) {
            case "times" -> "times";
            case "courier" -> "courier";
            default -> "helvetica";
        };
        boolean bold = o.path("bold").asBoolean(false);
        boolean italic = o.path("italic").asBoolean(false);
        float size = (float) o.path("size").asDouble(16);
        List<String> lines = Arrays.stream(o.path("text").asText("").replace("\r", "").replace("\t", "    ").split("\n", -1))
                .map(l -> l.replaceAll("\\p{Cntrl}", ""))
                .toList();
        if (lines.stream().allMatch(String::isBlank)) {
            return;
        }

        PDFont standard = EditorFonts.ForDocument.standard(family, bold, italic);
        PDFont font = standard;
        boolean fakeBold = false;
        if (!lines.stream().allMatch(l -> EditorFonts.ForDocument.canEncode(standard, l))) {
            font = fonts.unicodeFor(lines);
            if (font == null) {
                throw new ProcessingException("UNSUPPORTED_TEXT",
                        "Some characters in your text can't be added on this server yet. Latin text works everywhere.");
            }
            fakeBold = bold; // Unicode fonts have one weight: draw bold as fill + outline
        }

        Color color = color(o, "color", Color.BLACK);
        place(o);
        alpha(opacity(o, 1f), false);
        cs.setNonStrokingColor(color);
        if (fakeBold) {
            cs.setStrokingColor(color);
            cs.setLineWidth(size * 0.03f);
        }
        float baseline = BASELINE.get(family) * size;
        cs.beginText();
        cs.setFont(font, size);
        if (fakeBold) {
            cs.setRenderingMode(RenderingMode.FILL_STROKE);
        }
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).isEmpty()) {
                continue;
            }
            // y points down here: flip the text matrix so glyphs stay upright
            cs.setTextMatrix(new Matrix(1, 0, 0, -1, 0, baseline + i * size * LINE_HEIGHT));
            cs.showText(lines.get(i));
        }
        cs.endText();
    }

    private void box(JsonNode o, Color fill, Color stroke, float strokeWidth, float opacity, boolean multiply)
            throws IOException {
        float x = (float) o.path("x").asDouble();
        float y = (float) o.path("y").asDouble();
        float w = (float) o.path("w").asDouble();
        float h = (float) o.path("h").asDouble();
        alpha(opacity, multiply);
        if (fill != null) {
            cs.setNonStrokingColor(fill);
            cs.addRect(x, y, w, h);
            cs.fill();
        }
        if (stroke != null && strokeWidth > 0) {
            cs.setStrokingColor(stroke);
            cs.setLineWidth(strokeWidth);
            cs.setLineJoinStyle(0);
            // keep the outline inside the box, as the editor draws it
            float inset = strokeWidth / 2;
            cs.addRect(x + inset, y + inset, Math.max(0, w - strokeWidth), Math.max(0, h - strokeWidth));
            cs.stroke();
        }
    }

    private void line(JsonNode o, boolean arrow) throws IOException {
        float x1 = (float) o.path("x1").asDouble();
        float y1 = (float) o.path("y1").asDouble();
        float x2 = (float) o.path("x2").asDouble();
        float y2 = (float) o.path("y2").asDouble();
        float width = (float) Math.max(0.25, o.path("width").asDouble(2));
        Color color = color(o, "color", Color.BLACK);
        alpha(opacity(o, 1f), false);
        cs.setStrokingColor(color);
        cs.setNonStrokingColor(color);
        cs.setLineWidth(width);
        cs.setLineCapStyle(1);

        double angle = Math.atan2(y2 - y1, x2 - x1);
        float head = Math.max(8, width * 4);
        // stop the shaft at the arrowhead's base so the tip stays sharp
        float endX = arrow ? (float) (x2 - Math.cos(angle) * head * 0.8) : x2;
        float endY = arrow ? (float) (y2 - Math.sin(angle) * head * 0.8) : y2;
        cs.moveTo(x1, y1);
        cs.lineTo(endX, endY);
        cs.stroke();
        if (arrow) {
            double spread = Math.toRadians(25);
            cs.moveTo(x2, y2);
            cs.lineTo((float) (x2 - head * Math.cos(angle - spread)), (float) (y2 - head * Math.sin(angle - spread)));
            cs.lineTo((float) (x2 - head * Math.cos(angle + spread)), (float) (y2 - head * Math.sin(angle + spread)));
            cs.closePath();
            cs.fill();
        }
    }

    private void ink(JsonNode o) throws IOException {
        JsonNode points = o.path("points");
        alpha(opacity(o, 1f), false);
        cs.setStrokingColor(color(o, "color", Color.BLACK));
        cs.setLineWidth((float) Math.max(0.25, o.path("width").asDouble(2)));
        cs.setLineCapStyle(1);
        cs.setLineJoinStyle(1);
        cs.moveTo((float) points.get(0).get(0).asDouble(), (float) points.get(0).get(1).asDouble());
        if (points.size() == 1) {
            // a single click: a dot
            cs.lineTo((float) points.get(0).get(0).asDouble() + 0.01f, (float) points.get(0).get(1).asDouble());
        }
        for (int i = 1; i < points.size(); i++) {
            cs.lineTo((float) points.get(i).get(0).asDouble(), (float) points.get(i).get(1).asDouble());
        }
        cs.stroke();
    }

    private void image(JsonNode o, PDImageXObject image) throws IOException {
        float w = (float) o.path("w").asDouble();
        float h = (float) o.path("h").asDouble();
        place(o);
        alpha(opacity(o, 1f), false);
        // the image's unit square, flipped because y points down
        cs.drawImage(image, new Matrix(w, 0, 0, -h, 0, h));
    }

    /** Moves the origin to the object's top-left corner and applies its clockwise turn. */
    private void place(JsonNode o) throws IOException {
        AffineTransform t = AffineTransform.getTranslateInstance(o.path("x").asDouble(), o.path("y").asDouble());
        t.rotate(Math.toRadians(o.path("angle").asInt(0))); // y down: positive = clockwise, like CSS
        cs.transform(new Matrix(t));
    }

    private void alpha(float opacity, boolean multiply) throws IOException {
        if (opacity >= 1f && !multiply) {
            return;
        }
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(opacity);
        gs.setStrokingAlphaConstant(opacity);
        if (multiply) {
            gs.setBlendMode(BlendMode.MULTIPLY); // highlighter: text under it stays dark
        }
        cs.setGraphicsStateParameters(gs);
    }

    private static float opacity(JsonNode o, float fallback) {
        JsonNode n = o.path("opacity");
        return n.isNumber() ? (float) Math.max(0, Math.min(1, n.asDouble())) : fallback;
    }

    /** "#rrggbb" -> Color; missing -> fallback; "none" or "transparent" -> null. */
    static Color color(JsonNode o, String field, Color fallback) {
        String v = o.path(field).asText("");
        if (v.isEmpty()) {
            return fallback;
        }
        if (v.equals("none") || v.equals("transparent")) {
            return null;
        }
        if (!v.matches("#[0-9a-fA-F]{6}")) {
            throw EditPdfProcessor.invalid("Invalid color \"" + v + "\".");
        }
        return new Color(Integer.parseInt(v.substring(1), 16));
    }

    @Override
    public void close() throws IOException {
        cs.close();
    }
}
