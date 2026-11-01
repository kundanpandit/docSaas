package com.docsaas.api.processing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.docsaas.api.enums.FileKind;
import com.docsaas.api.enums.ToolCode;

/**
 * Converts JPG/PNG to WEBP, or WEBP to PNG/JPG.
 * Options: format = webp | png | jpg, quality = lossless | high | medium (WEBP output only)
 */
@Component
public class WebpConverterProcessor implements ToolProcessor {

    @Override
    public ToolCode tool() {
        return ToolCode.WEBP_CONVERTER;
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ProcessingContext.choice(options, "format", "webp", "webp", "png", "jpg");
        ProcessingContext.choice(options, "quality", "high", "lossless", "high", "medium");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        String format = ctx.choice("format", "webp", "webp", "png", "jpg");
        String quality = ctx.choice("quality", "high", "lossless", "high", "medium");

        if (input.kind() == FileKind.WEBP && format.equals("webp")) {
            throw new ProcessingException("INVALID_OPTION", "This image is already WEBP. Choose PNG or JPG as the output.");
        }

        BufferedImage image = ImageSupport.read(input);
        String base = FileNames.baseName(input.originalName());
        Path out = ctx.workDir().resolve("converted." + format);

        switch (format) {
            case "png" -> ImageSupport.writePng(image, out);
            case "jpg" -> ImageSupport.writeJpeg(image, 0.92f, out);
            default -> ImageSupport.writeWebp(image, quality.equals("lossless"), quality.equals("high") ? 0.85f : 0.7f, out);
        }

        String mime = switch (format) {
            case "png" -> "image/png";
            case "jpg" -> "image/jpeg";
            default -> "image/webp";
        };
        String summary = "Converted to " + format.toUpperCase() + " (" + FileNames.humanSize(Files.size(input.path()))
                + " → " + FileNames.humanSize(Files.size(out)) + ").";
        if (format.equals("jpg") && image.getColorModel().hasAlpha()) {
            summary += " Transparent areas were filled with white.";
        }
        return new ProcessingResult(out, base + "." + format, mime, summary);
    }
}
