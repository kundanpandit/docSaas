package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.processing.engine.AiClient;
import com.docsaas.api.processing.engine.DocumentIndex;
import com.docsaas.api.processing.engine.PdfTextExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Prepares a PDF for chat: extracts page text (OCR for scans) into a page-tagged index.
 * Questions are answered by ChatService against this index.
 */
@Component
public class ChatWithPdfProcessor implements ToolProcessor {

    private final AiClient ai;
    private final PdfTextExtractor extractor;
    private final ObjectMapper objectMapper;

    @Value("${docsaas.ai.max-pages:100}")
    private int maxPages;

    public ChatWithPdfProcessor(AiClient ai, PdfTextExtractor extractor, ObjectMapper objectMapper) {
        this.ai = ai;
        this.extractor = extractor;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolCode tool() {
        return ToolCode.CHAT_WITH_PDF;
    }

    @Override
    public String unavailableReason() {
        return ai.unavailableReason();
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);

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
                    "No readable text was found in this PDF, so there is nothing to chat about.");
        }

        Path out = ctx.workDir().resolve("index.json");
        objectMapper.writeValue(out.toFile(), index);

        String note = index.pages() < totalPages ? " (first " + index.pages() + " of " + totalPages + " pages)" : "";
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + "_text.json", "application/json",
                "Ready to chat about \"" + input.originalName() + "\"" + note + ". Ask anything about it.");
    }
}
