package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/**
 * One page per image, in upload order.
 * Options:
 *   pageSize    = fit (page matches image) | a4 | letter
 *   orientation = auto | portrait | landscape   (ignored for "fit")
 *   margin      = none | small | large           (ignored for "fit")
 */
@Component
public class ImagesToPdfProcessor implements ToolProcessor {

    @Override
    public ToolCode tool() {
        return ToolCode.IMAGES_TO_PDF;
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ProcessingContext.choice(options, "pageSize", "a4", "fit", "a4", "letter");
        ProcessingContext.choice(options, "orientation", "auto", "auto", "portrait", "landscape");
        ProcessingContext.choice(options, "margin", "small", "none", "small", "large");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        String pageSize = ctx.choice("pageSize", "a4", "fit", "a4", "letter");
        String orientation = ctx.choice("orientation", "auto", "auto", "portrait", "landscape");
        float margin = switch (ctx.choice("margin", "small", "none", "small", "large")) {
            case "none" -> 0f;
            case "large" -> 56f;
            default -> 20f;
        };

        Path out = ctx.workDir().resolve("images.pdf");
        PDDocument doc = PdfSupport.newDocument();
        try {
            for (ProcessingContext.InputFile input : ctx.inputs()) {
                PDImageXObject image = ImageSupport.embed(doc, input);
                float iw = image.getWidth();
                float ih = image.getHeight();

                PDRectangle box;
                if (pageSize.equals("fit")) {
                    box = new PDRectangle(iw, ih);
                } else {
                    PDRectangle base = pageSize.equals("letter") ? PDRectangle.LETTER : PDRectangle.A4;
                    boolean landscape = orientation.equals("landscape")
                            || (orientation.equals("auto") && iw > ih);
                    box = landscape ? new PDRectangle(base.getHeight(), base.getWidth()) : base;
                }

                PDPage page = new PDPage(box);
                doc.addPage(page);

                float pad = pageSize.equals("fit") ? 0 : margin;
                float availW = box.getWidth() - 2 * pad;
                float availH = box.getHeight() - 2 * pad;
                // Fit inside the printable area, keeping aspect ratio.
                float scale = Math.min(availW / iw, availH / ih);
                float w = iw * scale;
                float h = ih * scale;
                float x = (box.getWidth() - w) / 2;
                float y = (box.getHeight() - h) / 2;

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(image, x, y, w, h);
                }
            }
            PdfSupport.save(doc, out);
        } finally {
            PdfSupport.close(doc);
        }

        String name = ctx.inputs().size() == 1
                ? FileNames.baseName(ctx.inputs().get(0).originalName()) + ".pdf"
                : "images.pdf";
        return new ProcessingResult(out, name, "application/pdf",
                ctx.inputs().size() + (ctx.inputs().size() == 1 ? " image" : " images") + " converted to PDF.");
    }

}
