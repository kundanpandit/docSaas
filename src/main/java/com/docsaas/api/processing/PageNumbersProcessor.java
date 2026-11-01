package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/**
 * Adds page numbers to every page.
 *
 * Options: position = bottom-center | bottom-right | bottom-left | top-center | top-right | top-left;
 * format = number ("3") | of-total ("3 of 10") | page ("Page 3"); start = first number (1-9999)
 */
@Component
public class PageNumbersProcessor implements ToolProcessor {

    private static final String[] POSITIONS = { "bottom-center", "bottom-right", "bottom-left", "top-center",
            "top-right", "top-left" };
    private static final float MARGIN = 28;
    private static final float SIZE = 10;

    @Override
    public ToolCode tool() {
        return ToolCode.PAGE_NUMBERS;
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ProcessingContext.choice(options, "position", "bottom-center", POSITIONS);
        ProcessingContext.choice(options, "format", "number", "number", "of-total", "page");
        start(options);
    }

    private static int start(Map<String, String> options) {
        String raw = ProcessingContext.option(options, "start", "1");
        try {
            int start = Integer.parseInt(raw);
            if (start >= 1 && start <= 9999) {
                return start;
            }
        } catch (NumberFormatException ignored) {
            // handled below
        }
        throw new ProcessingException("INVALID_OPTION", "The first page number must be between 1 and 9999.");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        String position = ctx.choice("position", "bottom-center", POSITIONS);
        String format = ctx.choice("format", "number", "number", "of-total", "page");
        int start = start(ctx.options());
        boolean top = position.startsWith("top");
        float align = position.endsWith("center") ? 0.5f : position.endsWith("right") ? 1f : 0f;

        Path out = ctx.workDir().resolve("numbered.pdf");
        PDDocument doc = PdfSupport.load(input);
        try {
            int total = doc.getNumberOfPages();
            int last = start + total - 1;
            for (int i = 0; i < total; i++) {
                int n = start + i;
                String label = switch (format) {
                    case "of-total" -> n + " of " + last;
                    case "page" -> "Page " + n;
                    default -> String.valueOf(n);
                };
                try (PageOverlay overlay = new PageOverlay(doc, doc.getPage(i))) {
                    float x = MARGIN + (overlay.width() - 2 * MARGIN) * align;
                    float y = top ? overlay.height() - MARGIN - SIZE : MARGIN;
                    overlay.text(label, PageOverlay.FONT, SIZE, x, y, align, false, 0, 1f, 0.2f);
                }
            }
            PdfSupport.save(doc, out);
            return new ProcessingResult(out, FileNames.baseName(input.originalName()) + "_numbered.pdf",
                    "application/pdf", "Numbered " + total + (total == 1 ? " page" : " pages") + " (" + start + "-" + last + ").");
        } finally {
            PdfSupport.close(doc);
        }
    }
}
