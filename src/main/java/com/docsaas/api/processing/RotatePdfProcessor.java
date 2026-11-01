package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/**
 * Rotates pages clockwise, losslessly (only the page's /Rotate entry changes).
 *
 * Options: angle = 90 | 180 | 270; pages = all | ranges; ranges = e.g. "1-3, 5" (when pages=ranges)
 */
@Component
public class RotatePdfProcessor implements ToolProcessor {

    @Override
    public ToolCode tool() {
        return ToolCode.ROTATE_PDF;
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ProcessingContext.choice(options, "angle", "90", "90", "180", "270");
        if (ProcessingContext.choice(options, "pages", "all", "all", "ranges").equals("ranges")) {
            PageRanges.parse(options.get("ranges"));
        }
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        int angle = Integer.parseInt(ctx.choice("angle", "90", "90", "180", "270"));
        boolean all = ctx.choice("pages", "all", "all", "ranges").equals("all");

        Path out = ctx.workDir().resolve("rotated.pdf");
        PDDocument doc = PdfSupport.load(input);
        int rotated = 0;
        try {
            int pageCount = doc.getNumberOfPages();
            boolean[] selected = new boolean[pageCount + 1];
            if (all) {
                java.util.Arrays.fill(selected, true);
            } else {
                List<PageRanges.Range> ranges = PageRanges.parse(ctx.option("ranges", ""), pageCount);
                for (PageRanges.Range r : ranges) {
                    for (int p = r.from(); p <= r.to(); p++) {
                        selected[p] = true;
                    }
                }
            }
            for (int p = 1; p <= pageCount; p++) {
                if (selected[p]) {
                    PDPage page = doc.getPage(p - 1);
                    page.setRotation(((page.getRotation() + angle) % 360 + 360) % 360);
                    rotated++;
                }
            }
            PdfSupport.save(doc, out);
        } finally {
            PdfSupport.close(doc);
        }
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + "_rotated.pdf", "application/pdf",
                rotated + (rotated == 1 ? " page" : " pages") + " rotated " + angle + "° clockwise.");
    }
}
