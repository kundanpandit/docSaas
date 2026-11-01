package com.docsaas.api.processing.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.docsaas.api.processing.ProcessingException;

/**
 * Word to PDF via LibreOffice in headless mode (free). Each conversion runs in its own
 * process with a private user profile, so conversions can run in parallel safely.
 */
@Service
public class LibreOfficeEngine {

    private static final Logger log = LoggerFactory.getLogger(LibreOfficeEngine.class);

    private static final List<String> CANDIDATES = List.of(
            "C:/Program Files/LibreOffice/program/soffice.exe",
            "C:/Program Files (x86)/LibreOffice/program/soffice.exe",
            "/usr/bin/soffice",
            "/usr/lib/libreoffice/program/soffice",
            "/opt/libreoffice/program/soffice",
            "/Applications/LibreOffice.app/Contents/MacOS/soffice");

    /** LibreOffice is memory-hungry; cap concurrent conversions per instance. */
    private final Semaphore slots = new Semaphore(2);

    @Value("${docsaas.libreoffice.path:}")
    private String configuredPath;

    @Value("${docsaas.libreoffice.timeout-seconds:120}")
    private long timeoutSeconds;

    public Optional<Path> soffice() {
        if (configuredPath != null && !configuredPath.isBlank()) {
            Path p = Path.of(configuredPath);
            return Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
        }
        return CANDIDATES.stream().map(Path::of).filter(Files::isRegularFile).findFirst();
    }

    public String unavailableReason() {
        return soffice().isEmpty() ? "Word to PDF is not set up on this server yet." : null;
    }

    /** Converts {@code input} (which must have the right extension) to PDF inside {@code workDir}. */
    public Path convertToPdf(Path input, Path workDir) throws IOException {
        Path soffice = soffice().orElseThrow(() ->
                new ProcessingException("ENGINE_UNAVAILABLE", "Word to PDF is not set up on this server yet."));
        Path outDir = Files.createDirectories(workDir.resolve("lo-out"));
        Path profile = workDir.resolve("lo-profile").toAbsolutePath();

        List<String> command = List.of(
                soffice.toString(),
                "-env:UserInstallation=" + profile.toUri(),
                "--headless", "--norestore", "--nolockcheck", "--nodefault", "--nologo",
                "--convert-to", "pdf",
                "--outdir", outDir.toAbsolutePath().toString(),
                input.toAbsolutePath().toString());

        boolean acquired = false;
        Process process = null;
        try {
            acquired = slots.tryAcquire(timeoutSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                // Transient: let the job engine retry later
                throw new IOException("LibreOffice busy");
            }
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(workDir.resolve("lo.log").toFile())
                    .start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new ProcessingException("CONVERSION_TIMEOUT",
                        "The document took too long to convert. Try a smaller file.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while converting", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (acquired) {
                slots.release();
            }
        }

        String base = input.getFileName().toString().replaceFirst("\\.[^.]+$", "");
        Path pdf = outDir.resolve(base + ".pdf");
        if (!Files.exists(pdf) || Files.size(pdf) == 0) {
            log.warn("LibreOffice produced no output (exit {})", process.exitValue());
            throw new ProcessingException("CONVERSION_FAILED",
                    "This document could not be converted. It may be damaged or password protected.");
        }
        return pdf;
    }
}
