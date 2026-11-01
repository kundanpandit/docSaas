package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.processing.engine.LibreOfficeEngine;

/** DOCX/DOC to PDF using LibreOffice headless. */
@Component
public class WordToPdfProcessor implements ToolProcessor {

    private final LibreOfficeEngine libreOffice;

    public WordToPdfProcessor(LibreOfficeEngine libreOffice) {
        this.libreOffice = libreOffice;
    }

    @Override
    public ToolCode tool() {
        return ToolCode.WORD_TO_PDF;
    }

    @Override
    public String unavailableReason() {
        return libreOffice.unavailableReason();
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);

        // LibreOffice picks the import filter from the extension, so give it the real one.
        Path named = Files.copy(input.path(), ctx.workDir().resolve("document." + input.kind().getExtension()));
        Path pdf = libreOffice.convertToPdf(named, ctx.workDir());

        int pages;
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf.toFile())) {
            pages = doc.getNumberOfPages();
        }
        return new ProcessingResult(pdf, FileNames.baseName(input.originalName()) + ".pdf", "application/pdf",
                "Converted to a " + pages + "-page PDF.");
    }
}
