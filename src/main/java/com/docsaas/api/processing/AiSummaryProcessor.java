package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.processing.engine.AiClient;
import com.docsaas.api.processing.engine.AiClient.Message;
import com.docsaas.api.processing.engine.DocumentIndex;
import com.docsaas.api.processing.engine.PdfTextExtractor;

/**
 * AI summary of a PDF. Short documents are summarized in one call; long ones are summarized
 * in parts first and then combined (map-reduce), so any document fits the model's context.
 * Options: length = short | detailed
 */
@Component
public class AiSummaryProcessor implements ToolProcessor {

    private static final String SYSTEM = """
            You summarize documents for busy readers.
            Rules:
            - Use only information that appears in the provided document text. Never invent facts, names, numbers or dates.
            - When the text is unclear or incomplete, say so instead of guessing.
            - Cite page numbers like (p. 3) for key facts, using the [Page N] markers in the text.
            - Write in the same language as the document.
            - Output Markdown only, no preamble.""";

    private final AiClient ai;
    private final PdfTextExtractor extractor;

    @Value("${docsaas.ai.max-pages:100}")
    private int maxPages;

    @Value("${docsaas.ai.max-input-chars:120000}")
    private int maxInputChars;

    public AiSummaryProcessor(AiClient ai, PdfTextExtractor extractor) {
        this.ai = ai;
        this.extractor = extractor;
    }

    @Override
    public ToolCode tool() {
        return ToolCode.AI_SUMMARY;
    }

    @Override
    public String unavailableReason() {
        return ai.unavailableReason();
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ProcessingContext.choice(options, "length", "short", "short", "detailed");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        boolean detailed = ctx.choice("length", "short", "short", "detailed").equals("detailed");

        DocumentIndex index;
        int totalPages;
        PDDocument doc = PdfSupport.load(input);
        try {
            totalPages = doc.getNumberOfPages();
            index = DocumentIndex.build(extractor.extract(doc, maxPages));
        } finally {
            PdfSupport.close(doc);
        }
        if (index.totalChars() < 40) {
            throw new ProcessingException("NO_TEXT_FOUND",
                    "No readable text was found in this PDF. If it is scanned, try Scanned PDF OCR first.");
        }

        String format = detailed
                ? "Write: a 2-3 sentence overview, then '## Key points' with 6-10 bullets, then '## Details' with short sections for the main topics, then '## Action items' if the document asks the reader to do anything."
                : "Write: a 2-3 sentence overview, then '## Key points' with 4-6 bullets.";

        String summary;
        if (index.totalChars() <= maxInputChars) {
            summary = ai.complete(List.of(
                    Message.system(SYSTEM),
                    Message.user(format + "\n\nDocument:\n\n" + DocumentIndex.asExcerpts(index.chunks()))), 2048);
        } else {
            summary = mapReduce(index, format);
        }

        String disclosure = "\n\n---\n*AI-generated summary of \"" + input.originalName() + "\" ("
                + index.pages() + " of " + totalPages + " pages). Check important details against the original.*\n";
        Path out = ctx.workDir().resolve("summary.md");
        Files.writeString(out, summary + disclosure, StandardCharsets.UTF_8);

        String note = index.pages() < totalPages
                ? " Only the first " + index.pages() + " pages were summarized."
                : "";
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + "_summary.md",
                "text/markdown; charset=utf-8", "AI summary of " + index.pages() + " pages is ready." + note);
    }

    /** Summarize parts, then summarize the summaries. */
    private String mapReduce(DocumentIndex index, String format) {
        int partBudget = Math.max(20_000, maxInputChars / 4);
        List<List<DocumentIndex.Chunk>> parts = new ArrayList<>();
        List<DocumentIndex.Chunk> current = new ArrayList<>();
        int used = 0;
        for (DocumentIndex.Chunk c : index.chunks()) {
            if (used + c.text().length() > partBudget && !current.isEmpty()) {
                parts.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(c);
            used += c.text().length();
        }
        if (!current.isEmpty()) {
            parts.add(current);
        }

        StringBuilder notes = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            String partNotes = ai.complete(List.of(
                    Message.system(SYSTEM),
                    Message.user("This is part " + (i + 1) + " of " + parts.size() + " of a long document. "
                            + "List its important facts as concise bullets with page citations.\n\n"
                            + DocumentIndex.asExcerpts(parts.get(i)))), 1200);
            notes.append("### Part ").append(i + 1).append("\n").append(partNotes).append("\n\n");
        }

        return ai.complete(List.of(
                Message.system(SYSTEM),
                Message.user("Below are notes taken from each part of one long document. Combine them into a single summary. "
                        + format + "\n\nNotes:\n\n" + notes)), 2048);
    }
}
