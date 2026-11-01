package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.FileKind;
import com.docsaas.api.enums.ToolCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The PDF editor. The browser sends the PDF, any images used, and a JSON plan (the "data" part):
 *
 * <pre>
 * { "pages":   [ { "source": 3, "rotate": 90 } | { "blank": true, "width": 595, "height": 842, "rotate": 0 } ],
 *   "objects": [ { "page": 0, "type": "text" | "whiteout" | "highlight" | "rect" | "line" | "arrow" | "ink" | "image", ... } ] }
 * </pre>
 *
 * "pages" is the output page order (reordered, duplicated, deleted, blank pages inserted); "rotate"
 * is added to the page's own rotation. Objects are drawn in the page's coordinates as displayed
 * before that extra rotation, in points from the top-left corner (exactly what the editor shows),
 * so they turn with the page like ink on paper. See {@link EditCanvas} for each object's fields.
 */
@Component
public class EditPdfProcessor implements ToolProcessor {

    static final int MAX_OBJECTS = 5000;
    static final int MAX_INK_POINTS = 100_000;
    static final Set<String> TYPES = Set.of("text", "whiteout", "highlight", "rect", "line", "arrow", "ink", "image");

    private final EditorFonts fonts;
    private final ObjectMapper objectMapper;

    public EditPdfProcessor(EditorFonts fonts, ObjectMapper objectMapper) {
        this.fonts = fonts;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolCode tool() {
        return ToolCode.EDIT_PDF;
    }

    @Override
    public long maxDataBytes() {
        return 8L * 1024 * 1024;
    }

    @Override
    public void validateData(JsonNode data) {
        if (data == null) {
            throw new ProcessingException("NO_CHANGES", "Nothing to save: no edit data was sent.");
        }
        parsePages(data, Integer.MAX_VALUE);
        checkObjects(data, data.path("pages").size(), Integer.MAX_VALUE);
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        List<ProcessingContext.InputFile> inputs = ctx.inputs();
        ProcessingContext.InputFile pdf = inputs.get(0);
        if (pdf.kind() != FileKind.PDF || inputs.stream().skip(1).anyMatch(i -> !i.kind().isImage())) {
            throw new ProcessingException("INVALID_FILES", "The editor needs one PDF, optionally followed by images.");
        }
        if (ctx.data() == null) {
            throw new ProcessingException("NO_CHANGES", "Nothing to save: no edit data was sent.");
        }
        JsonNode data = objectMapper.readTree(ctx.data().toFile());

        Path out = ctx.workDir().resolve("edited.pdf");
        PDDocument src = PdfSupport.load(pdf);
        PDDocument doc = PdfSupport.newDocument();
        EditorFonts.ForDocument docFonts = fonts.forDocument(doc);
        int changes;
        try {
            List<PageSpec> pages = parsePages(data, src.getNumberOfPages());
            List<List<JsonNode>> objectsByPage = checkObjects(data, pages.size(), inputs.size() - 1);

            Map<Integer, PDImageXObject> images = new HashMap<>();
            changes = 0;
            for (int i = 0; i < pages.size(); i++) {
                PageSpec spec = pages.get(i);
                PDPage page;
                if (spec.source() > 0) {
                    page = PdfSupport.importPage(doc, src.getPage(spec.source() - 1));
                } else {
                    page = new PDPage(new PDRectangle(spec.width(), spec.height()));
                    doc.addPage(page);
                }
                List<JsonNode> objects = objectsByPage.get(i);
                // whiteout really removes the text under it (copy and search no longer find it)
                TextEraser.erase(doc, page, whiteoutAreas(page, objects));
                if (!objects.isEmpty()) {
                    try (EditCanvas canvas = new EditCanvas(doc, page, docFonts)) {
                        for (JsonNode o : objects) {
                            PDImageXObject image = null;
                            if (o.path("type").asText().equals("image")) {
                                int index = o.path("image").asInt();
                                image = images.get(index);
                                if (image == null) {
                                    image = ImageSupport.embed(doc, inputs.get(index));
                                    images.put(index, image);
                                }
                            }
                            canvas.draw(o, image);
                            changes++;
                        }
                    }
                }
                // Objects were drawn in the page as displayed before the user's rotation: turn them with it
                page.setRotation(((page.getRotation() + spec.rotate()) % 360 + 360) % 360);
            }
            PdfSupport.save(doc, out);
        } finally {
            PdfSupport.close(doc);
            PdfSupport.close(src);
            docFonts.close();
        }

        String name = FileNames.baseName(pdf.originalName()) + "_edited.pdf";
        int pageCount = data.path("pages").size();
        return new ProcessingResult(out, name, "application/pdf",
                "Saved " + changes + (changes == 1 ? " change" : " changes") + " in a " + pageCount
                        + (pageCount == 1 ? "-page" : "-page") + " PDF.");
    }

    /** Whiteout boxes of a page in its own user space (the page as stored, before rotation). */
    static List<java.awt.geom.Rectangle2D> whiteoutAreas(PDPage page, List<JsonNode> objects) {
        java.awt.geom.AffineTransform toUser = PdfSupport.displayToUser(page);
        toUser.concatenate(new java.awt.geom.AffineTransform(1, 0, 0, -1, 0, PdfSupport.displaySize(page)[1]));
        List<java.awt.geom.Rectangle2D> areas = new ArrayList<>();
        for (JsonNode o : objects) {
            if (o.path("type").asText().equals("whiteout") && !o.path("keepText").asBoolean(false)) {
                java.awt.geom.Rectangle2D box = new java.awt.geom.Rectangle2D.Double(
                        o.path("x").asDouble(), o.path("y").asDouble(), o.path("w").asDouble(), o.path("h").asDouble());
                areas.add(toUser.createTransformedShape(box).getBounds2D());
            }
        }
        return areas;
    }

    // ===== plan validation =====

    record PageSpec(int source, float width, float height, int rotate) {
    }

    static List<PageSpec> parsePages(JsonNode data, int sourcePages) {
        JsonNode pages = data.path("pages");
        if (!pages.isArray() || pages.isEmpty()) {
            throw invalid("The edited document must keep at least one page.");
        }
        if (pages.size() > PdfSupport.MAX_PAGES) {
            throw invalid("The edited document can have at most " + PdfSupport.MAX_PAGES + " pages.");
        }
        List<PageSpec> specs = new ArrayList<>();
        for (JsonNode p : pages) {
            int rotate = p.path("rotate").asInt(0);
            if (rotate % 90 != 0) {
                throw invalid("Pages can only be rotated in steps of 90 degrees.");
            }
            if (p.path("blank").asBoolean(false)) {
                float w = (float) p.path("width").asDouble(595);
                float h = (float) p.path("height").asDouble(842);
                if (w < 72 || h < 72 || w > 14400 || h > 14400) {
                    throw invalid("A blank page has an invalid size.");
                }
                specs.add(new PageSpec(0, w, h, rotate));
            } else {
                int source = p.path("source").asInt(0);
                if (source < 1 || source > sourcePages) {
                    throw invalid("The edit refers to a page that is not in the document.");
                }
                specs.add(new PageSpec(source, 0, 0, rotate));
            }
        }
        return specs;
    }

    /** Checks every object and groups them by output page, keeping their drawing order. */
    static List<List<JsonNode>> checkObjects(JsonNode data, int pageCount, int imageCount) {
        JsonNode objects = data.path("objects");
        if (!objects.isMissingNode() && !objects.isArray()) {
            throw invalid("The list of changes could not be read.");
        }
        if (objects.size() > MAX_OBJECTS) {
            throw invalid("Too many changes in one go (maximum " + MAX_OBJECTS + "). Please save in smaller steps.");
        }
        List<List<JsonNode>> byPage = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            byPage.add(new ArrayList<>());
        }
        int inkPoints = 0;
        for (JsonNode o : objects) {
            String type = o.path("type").asText("");
            if (!TYPES.contains(type)) {
                throw invalid("Unknown kind of change: \"" + type + "\".");
            }
            int page = o.path("page").asInt(-1);
            if (page < 0 || page >= pageCount) {
                throw invalid("A change refers to a page that is not in the document.");
            }
            if (type.equals("image")) {
                int index = o.path("image").asInt(0);
                if (index < 1 || index > imageCount) {
                    throw invalid("An image in the edit is missing. Please add it again.");
                }
            }
            if (type.equals("ink")) {
                inkPoints += o.path("points").size();
            }
            EditCanvas.check(o);
            byPage.get(page).add(o);
        }
        if (inkPoints > MAX_INK_POINTS) {
            throw invalid("The drawings are too detailed to save. Please simplify them.");
        }
        return byPage;
    }

    static ProcessingException invalid(String message) {
        return new ProcessingException("INVALID_EDIT", message);
    }
}
