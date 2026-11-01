package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/**
 * Stamps a text watermark on every page, on top of the content and semi-transparent so the
 * page stays readable.
 *
 * Options: text (1-60 chars, Latin); layout = diagonal | center; opacity = light | medium | strong
 */
@Component
public class WatermarkPdfProcessor implements ToolProcessor {

    static final int MAX_TEXT = 60;

    @Override
    public ToolCode tool() {
        return ToolCode.WATERMARK_PDF;
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        String text = ProcessingContext.option(options, "text", "");
        if (text.isEmpty() || text.length() > MAX_TEXT) {
            throw new ProcessingException("INVALID_TEXT", "Please enter watermark text of up to " + MAX_TEXT + " characters.");
        }
        PageOverlay.checkEncodable(text, "Watermark text");
        ProcessingContext.choice(options, "layout", "diagonal", "diagonal", "center");
        ProcessingContext.choice(options, "opacity", "medium", "light", "medium", "strong");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        validateOptions(ctx.options());
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        String text = ctx.option("text", "");
        boolean diagonal = ctx.choice("layout", "diagonal", "diagonal", "center").equals("diagonal");
        float opacity = switch (ctx.choice("opacity", "medium", "light", "medium", "strong")) {
            case "light" -> 0.15f;
            case "strong" -> 0.45f;
            default -> 0.28f;
        };

        Path out = ctx.workDir().resolve("watermarked.pdf");
        PDDocument doc = PdfSupport.load(input);
        try {
            float unitWidth = PageOverlay.BOLD.getStringWidth(text) / 1000f;
            for (PDPage page : doc.getPages()) {
                try (PageOverlay overlay = new PageOverlay(doc, page)) {
                    float w = overlay.width();
                    float h = overlay.height();
                    // Fit the text: diagonal spans ~80% of the diagonal, centered ~70% of the width
                    float span = diagonal ? (float) Math.hypot(w, h) * 0.8f : w * 0.7f;
                    float size = Math.max(10, Math.min(span / unitWidth, Math.min(w, h) / 4));
                    float angle = diagonal ? (float) Math.toDegrees(Math.atan2(h, w)) : 0;
                    overlay.text(text, PageOverlay.BOLD, size, w / 2, h / 2, 0.5f, true, angle, opacity, 0.35f);
                }
            }
            PdfSupport.save(doc, out);
        } finally {
            PdfSupport.close(doc);
        }
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + "_watermarked.pdf",
                "application/pdf", "Watermark added to every page.");
    }
}
