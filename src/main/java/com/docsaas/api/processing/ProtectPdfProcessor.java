package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/**
 * Encrypts a PDF with AES-256 so it needs a password to open. The same password is the owner
 * password, so whoever opens it keeps full rights (print, copy, edit).
 *
 * Options: password (4-64 characters). The password is never logged and is removed from the
 * stored job options once the job finishes.
 */
@Component
public class ProtectPdfProcessor implements ToolProcessor {

    static final int MIN_PASSWORD = 4;
    static final int MAX_PASSWORD = 64;

    @Override
    public ToolCode tool() {
        return ToolCode.PROTECT_PDF;
    }

    @Override
    public Set<String> sensitiveOptions() {
        return Set.of("password");
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        String password = options.get("password");
        if (password == null || password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            throw new ProcessingException("INVALID_PASSWORD",
                    "Please choose a password of " + MIN_PASSWORD + " to " + MAX_PASSWORD + " characters.");
        }
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        // Not trimmed: spaces are valid password characters
        String password = ctx.options().get("password");
        validateOptions(ctx.options());

        Path out = ctx.workDir().resolve("protected.pdf");
        PDDocument doc = PdfSupport.load(input);
        try {
            StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password, new AccessPermission());
            policy.setEncryptionKeyLength(256);
            doc.protect(policy);
            PdfSupport.save(doc, out);
        } finally {
            PdfSupport.close(doc);
        }
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + "_protected.pdf", "application/pdf",
                "Your PDF is now password protected (AES-256). Keep the password safe: it cannot be recovered.");
    }
}
