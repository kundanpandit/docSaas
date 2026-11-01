package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/**
 * Options:
 * mode = "ranges"  : one PDF per range (ZIP if more than one)
 *        "extract" : all selected pages in a single PDF
 *        "every"   : one PDF per page (ZIP)
 * ranges = e.g. "1-3, 5" (required for ranges/extract)
 */
@Component
public class SplitPdfProcessor implements ToolProcessor {

    @Override
    public ToolCode tool() {
        return ToolCode.SPLIT_PDF;
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        String mode = ProcessingContext.choice(options, "mode", "ranges", "ranges", "extract", "every");
        if (!mode.equals("every")) {
            PageRanges.parse(options.get("ranges"));
        }
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        String base = FileNames.baseName(input.originalName());
        String mode = ctx.choice("mode", "ranges", "ranges", "extract", "every");

        PDDocument src = PdfSupport.load(input);
        try {
            int pageCount = src.getNumberOfPages();
            List<PageRanges.Range> ranges;
            if (mode.equals("every")) {
                ranges = new ArrayList<>();
                for (int i = 1; i <= pageCount; i++) {
                    ranges.add(new PageRanges.Range(i, i));
                }
            } else {
                ranges = PageRanges.parse(ctx.option("ranges", ""), pageCount);
            }

            if (mode.equals("extract")) {
                Path out = ctx.workDir().resolve("extracted.pdf");
                int pages = writePages(src, ranges, out);
                return new ProcessingResult(out, base + "_extracted.pdf", "application/pdf",
                        pages + " pages extracted into a new PDF.");
            }

            if (ranges.size() == 1) {
                PageRanges.Range r = ranges.get(0);
                Path out = ctx.workDir().resolve("part.pdf");
                writePages(src, ranges, out);
                return new ProcessingResult(out, base + "_pages_" + r.label() + ".pdf", "application/pdf",
                        "Pages " + r.label() + " saved as a new PDF.");
            }

            List<ZipSupport.Entry> entries = new ArrayList<>();
            for (int i = 0; i < ranges.size(); i++) {
                PageRanges.Range r = ranges.get(i);
                Path part = ctx.workDir().resolve("part_" + i + ".pdf");
                writePages(src, List.of(r), part);
                entries.add(new ZipSupport.Entry(part, base + "_pages_" + r.label() + ".pdf"));
            }
            Path zip = ctx.workDir().resolve("split.zip");
            ZipSupport.zip(entries, zip);
            return new ProcessingResult(zip, base + "_split.zip", "application/zip",
                    "Split into " + entries.size() + " PDF files (ZIP).");
        } finally {
            PdfSupport.close(src);
        }
    }

    private static int writePages(PDDocument src, List<PageRanges.Range> ranges, Path out) throws IOException {
        PDDocument part = PdfSupport.newDocument();
        try {
            int count = 0;
            for (PageRanges.Range r : ranges) {
                for (int p = r.from(); p <= r.to(); p++) {
                    PdfSupport.importPage(part, src.getPage(p - 1));
                    count++;
                }
            }
            PdfSupport.save(part, out);
            return count;
        } finally {
            PdfSupport.close(part);
        }
    }

}
