package com.docsaas.api.processing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.docsaas.api.enums.FileKind;
import com.docsaas.api.enums.ToolCode;

/**
 * Keeps the input format. JPEG is re-encoded at the chosen quality; PNG is lossless,
 * so for PNG the size reduction comes mainly from resizing.
 * Options: level = low | medium | high, maxDimension = 0 (keep) | 3840 | 2560 | 1920 | 1280
 */
@Component
public class CompressImageProcessor implements ToolProcessor {

    @Override
    public ToolCode tool() {
        return ToolCode.COMPRESS_IMAGE;
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ProcessingContext.choice(options, "level", "medium", "low", "medium", "high");
        ProcessingContext.choice(options, "maxDimension", "0", "0", "3840", "2560", "1920", "1280");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        float quality = switch (ctx.choice("level", "medium", "low", "medium", "high")) {
            case "low" -> 0.85f;
            case "high" -> 0.5f;
            default -> 0.7f;
        };
        int maxDimension = Integer.parseInt(ctx.choice("maxDimension", "0", "0", "3840", "2560", "1920", "1280"));

        BufferedImage original = ImageSupport.read(input);
        BufferedImage image = ImageSupport.limitSize(original, maxDimension);
        boolean resized = image != original;
        String base = FileNames.baseName(input.originalName()) + "_compressed";

        Path out;
        String name;
        String mime;
        if (input.kind() == FileKind.WEBP) {
            out = ctx.workDir().resolve("compressed.webp");
            ImageSupport.writeWebp(image, false, quality, out);
            name = base + ".webp";
            mime = "image/webp";
        } else if (input.kind() == FileKind.JPEG) {
            out = ctx.workDir().resolve("compressed.jpg");
            ImageSupport.writeJpeg(image, quality, out);
            name = base + ".jpg";
            mime = "image/jpeg";
        } else {
            out = ctx.workDir().resolve("compressed.png");
            ImageSupport.writePng(image, out);
            name = base + ".png";
            mime = "image/png";
        }

        long before = Files.size(input.path());
        long after = Files.size(out);
        if (resized) {
            // The user asked for smaller dimensions: honour that even if bytes did not shrink.
            String dims = "Resized to " + image.getWidth() + "×" + image.getHeight() + " px. ";
            String size = after < before
                    ? FileNames.sizeChange(before, after)
                    : "File size: " + FileNames.humanSize(after) + ".";
            return new ProcessingResult(out, name, mime, dims + size);
        }
        if (after >= before) {
            Files.copy(input.path(), out, StandardCopyOption.REPLACE_EXISTING);
            after = before;
        }
        return new ProcessingResult(out, name, mime, FileNames.sizeChange(before, after));
    }
}
