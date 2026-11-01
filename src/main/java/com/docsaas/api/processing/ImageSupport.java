package com.docsaas.api.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public final class ImageSupport {

    /** Guards against decompression bombs: a tiny file can declare enormous dimensions. */
    public static final long MAX_PIXELS = 50_000_000L;

    private ImageSupport() {
    }

    public static BufferedImage read(ProcessingContext.InputFile input) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(input.path().toFile())) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) {
                throw invalid(input);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new ProcessingException("IMAGE_TOO_LARGE",
                            "\"" + input.originalName() + "\" has too many pixels (limit is 50 megapixels).");
                }
                return reader.read(0);
            } catch (ProcessingException e) {
                throw e;
            } catch (Exception e) {
                throw invalid(input);
            } finally {
                reader.dispose();
            }
        }
    }

    private static ProcessingException invalid(ProcessingContext.InputFile input) {
        return new ProcessingException("INVALID_IMAGE",
                "\"" + input.originalName() + "\" could not be read. The image may be damaged or use an unsupported variant.");
    }

    /** JPEG has no transparency: flatten onto white and drop alpha. */
    /**
     * Embeds an uploaded image in a PDF. JPEGs keep their original bytes (no re-encoding, no quality
     * loss); other formats are stored losslessly. Decoding first validates the file and the pixel limit.
     */
    public static org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject embed(
            org.apache.pdfbox.pdmodel.PDDocument doc, ProcessingContext.InputFile input) throws IOException {
        BufferedImage decoded = read(input);
        if (input.kind() == com.docsaas.api.enums.FileKind.JPEG) {
            try (java.io.InputStream in = Files.newInputStream(input.path())) {
                return org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromStream(doc, in);
            } catch (IOException | RuntimeException e) {
                return org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromImage(doc, toRgb(decoded), 0.92f);
            }
        }
        return org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, decoded);
    }

    public static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    /** Scales down so the longest side is at most maxDimension; returns src unchanged if already small enough. */
    public static BufferedImage limitSize(BufferedImage src, int maxDimension) {
        int longest = Math.max(src.getWidth(), src.getHeight());
        if (maxDimension <= 0 || longest <= maxDimension) {
            return src;
        }
        double scale = (double) maxDimension / longest;
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage out = new BufferedImage(w, h, type);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    public static void writeJpeg(BufferedImage image, float quality, Path target) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(Files.newOutputStream(target))) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(toRgb(image), null, null), param);
        } finally {
            writer.dispose();
        }
    }

    /**
     * WEBP via the webp-imageio plugin. quality 0..1 is used for lossy; lossless ignores it.
     */
    public static void writeWebp(BufferedImage image, boolean lossless, float quality, Path target) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IOException("No WEBP writer available");
        }
        ImageWriter writer = writers.next();
        com.luciad.imageio.webp.WebPWriteParam param = new com.luciad.imageio.webp.WebPWriteParam(writer.getLocale());
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionType(param.getCompressionTypes()[lossless
                ? com.luciad.imageio.webp.WebPWriteParam.LOSSLESS_COMPRESSION
                : com.luciad.imageio.webp.WebPWriteParam.LOSSY_COMPRESSION]);
        if (!lossless) {
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream out = ImageIO.createImageOutputStream(Files.newOutputStream(target))) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public static void writePng(BufferedImage image, Path target) throws IOException {
        if (!ImageIO.write(image, "png", target.toFile())) {
            throw new IOException("No PNG writer available");
        }
    }
}
