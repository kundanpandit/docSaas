package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/**
 * Removes the password and restrictions from a PDF. PDFs that need a password to open can only
 * be unlocked with that password; PDFs that only restrict printing/copying open without one.
 *
 * Options: password (optional). Never logged; removed from the stored job options afterwards.
 */
@Component
public class UnlockPdfProcessor implements ToolProcessor {

    @Override
    public ToolCode tool() {
        return ToolCode.UNLOCK_PDF;
    }

    @Override
    public Set<String> sensitiveOptions() {
        return Set.of("password");
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        String password = options.get("password");
        if (password != null && password.length() > ProtectPdfProcessor.MAX_PASSWORD * 2) {
            throw new ProcessingException("INVALID_PASSWORD", "The password is too long.");
        }
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        String password = ctx.options().getOrDefault("password", "");

        Path out = ctx.workDir().resolve("unlocked.pdf");
        PDDocument doc;
        try {
            doc = PdfSupport.loadWithPassword(input, password);
        } catch (ProcessingException e) {
            if (e.getCode().equals("PDF_ENCRYPTED") && password.isEmpty()) {
                throw new ProcessingException("PASSWORD_REQUIRED",
                        "This PDF needs a password to open. Enter the password to unlock it.");
            }
            throw e;
        }
        try {
            if (!doc.isEncrypted()) {
                throw new ProcessingException("NOT_ENCRYPTED", "This PDF is not protected, so there is nothing to unlock.");
            }
            doc.setAllSecurityToBeRemoved(true);
            PdfSupport.save(doc, out);
        } finally {
            PdfSupport.close(doc);
        }
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + "_unlocked.pdf", "application/pdf",
                "Password and restrictions removed.");
    }
}
