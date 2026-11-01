package com.docsaas.api.processing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/**
 * Re-encodes embedded raster images as JPEG (optionally downscaled) and saves with
 * compressed object streams. An image is only replaced when the result is smaller,
 * and the original file is returned if nothing could be reduced.
 *
 * Options: level = low | medium | high
 */
@Component
public class CompressPdfProcessor implements ToolProcessor {

    private static final int MIN_PIXELS = 10_000;

    private record Level(float quality, int maxDimension) {
    }

    @Override
    public ToolCode tool() {
        return ToolCode.COMPRESS_PDF;
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ProcessingContext.choice(options, "level", "medium", "low", "medium", "high");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        Level level = switch (ctx.choice("level", "medium", "low", "medium", "high")) {
            case "low" -> new Level(0.85f, 0);
            case "high" -> new Level(0.45f, 1400);
            default -> new Level(0.65f, 2200);
        };

        Path out = ctx.workDir().resolve("compressed.pdf");
        PDDocument doc = PdfSupport.load(input);
        try {
            Map<COSBase, PDXObject> replaced = new IdentityHashMap<>();
            Set<COSBase> visitedForms = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            for (PDPage page : doc.getPages()) {
                compressResources(doc, page.getResources(), level, replaced, visitedForms);
            }
            PdfSupport.save(doc, out);
        } finally {
            PdfSupport.close(doc);
        }

        long before = Files.size(input.path());
        long after = Files.size(out);
        if (after >= before) {
            Files.copy(input.path(), out, StandardCopyOption.REPLACE_EXISTING);
            after = before;
        }
        String name = FileNames.baseName(input.originalName()) + "_compressed.pdf";
        return new ProcessingResult(out, name, "application/pdf", FileNames.sizeChange(before, after));
    }

    private void compressResources(PDDocument doc, PDResources resources, Level level,
            Map<COSBase, PDXObject> replaced, Set<COSBase> visitedForms) throws IOException {
        if (resources == null) {
            return;
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xobject;
            try {
                xobject = resources.getXObject(name);
            } catch (IOException e) {
                continue; // unreadable object: leave it untouched
            }

            if (xobject instanceof PDImageXObject image) {
                COSBase key = image.getCOSObject();
                PDXObject replacement = replaced.get(key);
                if (replacement == null) {
                    replacement = recompress(doc, image, level);
                    replaced.put(key, replacement);
                }
                if (replacement != image) {
                    resources.put(name, replacement);
                }
            } else if (xobject instanceof PDFormXObject form && visitedForms.add(form.getCOSObject())) {
                compressResources(doc, form.getResources(), level, replaced, visitedForms);
            }
        }
    }

    /** Returns a smaller JPEG version of the image, or the original image if that would not help. */
    private PDXObject recompress(PDDocument doc, PDImageXObject image, Level level) {
        try {
            long pixels = (long) image.getWidth() * image.getHeight();
            // Skip masks, transparency, bilevel scans and tiny images: JPEG would damage or enlarge them.
            if (image.isStencil() || image.getBitsPerComponent() == 1 || pixels < MIN_PIXELS
                    || pixels > ImageSupport.MAX_PIXELS
                    || image.getSoftMask() != null || image.getMask() != null) {
                return image;
            }
            BufferedImage decoded = image.getImage();
            BufferedImage scaled = ImageSupport.limitSize(decoded, level.maxDimension());
            PDImageXObject jpeg = JPEGFactory.createFromImage(doc, ImageSupport.toRgb(scaled), level.quality());
            if (jpeg.getCOSObject().getLength() < image.getCOSObject().getLength()) {
                return jpeg;
            }
        } catch (IOException | RuntimeException e) {
            // Unsupported colour space or filter: keep the original image.
        }
        return image;
    }
}
