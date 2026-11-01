package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.processing.engine.OcrEngine;

/** OCR an image into plain text. Options: language = eng | hin | eng+hin ... */
@Component
public class ImageToTextProcessor implements ToolProcessor {

    private final OcrEngine ocr;

    public ImageToTextProcessor(OcrEngine ocr) {
        this.ocr = ocr;
    }

    @Override
    public ToolCode tool() {
        return ToolCode.IMAGE_TO_TEXT;
    }

    @Override
    public String unavailableReason() {
        return ocr.unavailableReason();
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ocr.resolveLanguage(options);
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        String language = ocr.resolveLanguage(ctx.options());

        String text = ocr.imageToText(ImageSupport.read(input), language);
        if (text.isBlank()) {
            throw new ProcessingException("NO_TEXT_FOUND",
                    "No readable text was found in this image. Try a sharper, well-lit image.");
        }

        Path out = ctx.workDir().resolve("text.txt");
        Files.writeString(out, text + System.lineSeparator(), StandardCharsets.UTF_8);
        int words = text.split("\\s+").length;
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + ".txt",
                "text/plain; charset=utf-8", "Extracted " + words + " words. OCR may contain mistakes - please review.");
    }
}
