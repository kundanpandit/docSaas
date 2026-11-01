package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

@Component
public class MergePdfProcessor implements ToolProcessor {

    @Override
    public ToolCode tool() {
        return ToolCode.MERGE_PDF;
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        List<PDDocument> sources = new ArrayList<>();
        PDDocument merged = PdfSupport.newDocument();
        try {
            int totalPages = 0;
            PDFMergerUtility merger = new PDFMergerUtility();
            // Inputs arrive in the order the user arranged them.
            for (ProcessingContext.InputFile input : ctx.inputs()) {
                PDDocument src = PdfSupport.load(input);
                sources.add(src);
                totalPages += src.getNumberOfPages();
                if (totalPages > PdfSupport.MAX_PAGES) {
                    throw new ProcessingException("TOO_MANY_PAGES",
                            "The merged document would exceed " + PdfSupport.MAX_PAGES + " pages.");
                }
                merger.appendDocument(merged, src);
            }

            Path out = ctx.workDir().resolve("merged.pdf");
            PdfSupport.save(merged, out);
            return new ProcessingResult(out, "merged.pdf", "application/pdf",
                    ctx.inputs().size() + " files merged into one PDF with " + totalPages + " pages.");
        } finally {
            PdfSupport.close(merged);
            sources.forEach(PdfSupport::close);
        }
    }
}
