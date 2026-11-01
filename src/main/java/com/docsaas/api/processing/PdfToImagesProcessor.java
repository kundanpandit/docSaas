package com.docsaas.api.processing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/** Options: format = png | jpg, dpi = 72 | 150 | 300 */
@Component
public class PdfToImagesProcessor implements ToolProcessor {

    public static final int MAX_RENDER_PAGES = 100;

    @Override
    public ToolCode tool() {
        return ToolCode.PDF_TO_IMAGES;
    }

    @Override
    public void validateOptions(Map<String, String> options) {
        ProcessingContext.choice(options, "format", "png", "png", "jpg");
        ProcessingContext.choice(options, "dpi", "150", "72", "150", "300");
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        String base = FileNames.baseName(input.originalName());
        String format = ctx.choice("format", "png", "png", "jpg");
        int dpi = Integer.parseInt(ctx.choice("dpi", "150", "72", "150", "300"));

        PDDocument doc = PdfSupport.load(input);
        try {
            int pages = doc.getNumberOfPages();
            if (pages > MAX_RENDER_PAGES) {
                throw new ProcessingException("TOO_MANY_PAGES",
                        "PDF to Images supports up to " + MAX_RENDER_PAGES + " pages. Split the document first.");
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            List<ZipSupport.Entry> entries = new ArrayList<>();
            for (int i = 0; i < pages; i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                Path file = ctx.workDir().resolve("page_" + (i + 1) + "." + format);
                if (format.equals("jpg")) {
                    ImageSupport.writeJpeg(img, 0.9f, file);
                } else {
                    ImageSupport.writePng(img, file);
                }
                entries.add(new ZipSupport.Entry(file, base + "_page_" + (i + 1) + "." + format));
            }

            String mime = format.equals("jpg") ? "image/jpeg" : "image/png";
            if (entries.size() == 1) {
                return new ProcessingResult(entries.get(0).file(), entries.get(0).name(), mime,
                        "1 page converted to " + format.toUpperCase() + ".");
            }
            Path zip = ctx.workDir().resolve("images.zip");
            ZipSupport.zip(entries, zip);
            return new ProcessingResult(zip, base + "_images.zip", "application/zip",
                    pages + " pages converted to " + format.toUpperCase() + " images (ZIP).");
        } finally {
            PdfSupport.close(doc);
        }
    }
}
