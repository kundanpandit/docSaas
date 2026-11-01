package com.docsaas.api.processing.engine;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.docsaas.api.processing.ProcessingContext;
import com.docsaas.api.processing.ProcessingException;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.ITesseract.RenderedFormat;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;

/**
 * Tesseract OCR (free, runs locally). Needs <lang>.traineddata files in docsaas.ocr.tessdata.
 * A new Tesseract instance is created per call because instances are not thread-safe.
 */
@Service
public class OcrEngine {

    private static final Logger log = LoggerFactory.getLogger(OcrEngine.class);

    /** Friendly names for the language picker; any installed traineddata is usable. */
    public static final Map<String, String> LANGUAGE_NAMES = Map.of(
            "eng", "English", "hin", "Hindi", "spa", "Spanish", "fra", "French", "deu", "German",
            "ara", "Arabic", "ben", "Bengali", "mar", "Marathi", "tam", "Tamil", "tel", "Telugu");

    private static final int MIN_OCR_WIDTH = 1600;

    @Value("${docsaas.ocr.tessdata}")
    private String tessdataDir;

    public Path tessdata() {
        return Path.of(tessdataDir).toAbsolutePath().normalize();
    }

    /** Language codes with a traineddata file present (excluding helper models like osd). */
    public Set<String> installedLanguages() {
        Set<String> langs = new TreeSet<>();
        Path dir = tessdata();
        if (!Files.isDirectory(dir)) {
            return langs;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".traineddata"))
                    .map(n -> n.substring(0, n.length() - ".traineddata".length()))
                    .filter(n -> !n.equals("osd") && !n.equals("equ"))
                    .forEach(langs::add);
        } catch (IOException e) {
            log.warn("Could not list tessdata directory");
        }
        return langs;
    }

    public String unavailableReason() {
        return installedLanguages().isEmpty() ? "OCR is not set up on this server yet." : null;
    }

    /** Validates a "+"-joined language option (e.g. "eng+hin") against installed languages. */
    public String resolveLanguage(Map<String, String> options) {
        Set<String> installed = installedLanguages();
        String requested = ProcessingContext.option(options, "language", installed.contains("eng") ? "eng" : "");
        if (requested.isEmpty() && !installed.isEmpty()) {
            requested = installed.iterator().next();
        }
        String[] parts = requested.split("\\+");
        if (parts.length > 3) {
            throw new ProcessingException("INVALID_OPTION", "Choose at most 3 OCR languages.");
        }
        for (String part : parts) {
            if (!installed.contains(part)) {
                throw new ProcessingException("INVALID_OPTION",
                        "OCR language \"" + part + "\" is not available. Available: " + String.join(", ", installed) + ".");
            }
        }
        return requested;
    }

    private ITesseract newTesseract(String language, int dpi) {
        Tesseract t = new Tesseract();
        t.setDatapath(tessdata().toString());
        t.setLanguage(language);
        t.setVariable("user_defined_dpi", String.valueOf(dpi));
        return t;
    }

    public String imageToText(BufferedImage image, String language) {
        try {
            BufferedImage prepared = prepare(image);
            return newTesseract(language, 300).doOCR(rotate(prepared, uprightTurn(prepared, language))).strip();
        } catch (TesseractException | Error e) {
            // UnsatisfiedLinkError etc. when the native library is missing
            log.error("OCR failed", e);
            throw new ProcessingException("OCR_FAILED", "Text recognition failed for this image.");
        }
    }

    /**
     * OCRs one page image into a PDF page that holds only the invisible text layer (no image), sized
     * like the image at {@code dpi}. Callers lay it over the original page, so the page keeps its
     * original quality and file size.
     */
    public Path pageTextLayer(Path pageImage, String language, int dpi, Path workDir, int index) {
        Tesseract tesseract = (Tesseract) newTesseract(language, dpi);
        tesseract.setVariable("textonly_pdf", "1");
        String base = workDir.resolve("ocr_text_" + index).toString();
        try {
            tesseract.createDocuments(pageImage.toString(), base, List.of(RenderedFormat.PDF));
        } catch (TesseractException | Error e) {
            log.error("OCR PDF rendering failed", e);
            throw new ProcessingException("OCR_FAILED", "Text recognition failed on page " + (index + 1) + ".");
        }
        Path pdf = Path.of(base + ".pdf");
        if (!Files.exists(pdf)) {
            throw new ProcessingException("OCR_FAILED", "Text recognition failed on page " + (index + 1) + ".");
        }
        return pdf;
    }

    /**
     * Grayscale, and small images (phone screenshots, thumbnails) upscaled so letters are tall enough
     * for Tesseract, which reads best at roughly 30 px per line of text or more.
     */
    static BufferedImage prepare(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        double scale = w < MIN_OCR_WIDTH ? Math.min(3.0, (double) MIN_OCR_WIDTH / w) : 1.0;
        int tw = (int) Math.round(w * scale);
        int th = (int) Math.round(h * scale);
        BufferedImage gray = new BufferedImage(tw, th, BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics2D g = gray.createGraphics();
        g.setColor(java.awt.Color.WHITE); // transparent PNGs: transparent areas become paper, not black
        g.fillRect(0, 0, tw, th);
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, tw, th, null);
        g.dispose();
        return gray;
    }

    /**
     * Clockwise turn (0, 90, 180, 270) that makes the text in the image upright. Reads the image as
     * is first; only when few words are recognized with confidence are the other three orientations
     * tried, and the one with the most confidently read text wins. Needs no extra language data.
     */
    public int uprightTurn(BufferedImage image, String language) {
        // Orientation only needs a rough read: cap the size to keep the extra passes fast
        double shrink = Math.min(1.0, 1600.0 / Math.max(image.getWidth(), image.getHeight()));
        BufferedImage probe = shrink < 1.0 ? scaled(image, shrink) : image;
        ITesseract tesseract = newTesseract(language, 300);

        Score upright = score(tesseract, probe);
        if (upright.confidentWords() >= 3 && upright.meanConfidence() >= 70) {
            return 0;
        }
        int best = 0;
        double bestScore = upright.value();
        for (int turn : new int[] { 90, 180, 270 }) {
            double s = score(tesseract, rotate(probe, turn)).value();
            if (s > bestScore * 1.3 && s > bestScore + 5) {
                best = turn;
                bestScore = s;
            }
        }
        if (best != 0) {
            log.debug("OCR orientation corrected by {} degrees", best);
        }
        return best;
    }

    private record Score(double value, int confidentWords, double meanConfidence) {
    }

    /** Characters of words read with at least 60% confidence: high for upright text, low for garbage. */
    private static Score score(ITesseract tesseract, BufferedImage image) {
        List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);
        double value = 0;
        int confident = 0;
        double confSum = 0;
        int counted = 0;
        for (Word w : words) {
            long alnum = w.getText().chars().filter(Character::isLetterOrDigit).count();
            if (alnum < 2) {
                continue;
            }
            counted++;
            confSum += w.getConfidence();
            if (w.getConfidence() >= 60) {
                confident++;
                value += alnum * w.getConfidence() / 100.0;
            }
        }
        return new Score(value, confident, counted == 0 ? 0 : confSum / counted);
    }

    /** The image turned clockwise by 0, 90, 180 or 270 degrees. */
    public static BufferedImage rotate(BufferedImage image, int degreesClockwise) {
        int turn = ((degreesClockwise % 360) + 360) % 360;
        if (turn == 0) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        boolean sideways = turn == 90 || turn == 270;
        int type = image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_RGB : image.getType();
        BufferedImage out = new BufferedImage(sideways ? h : w, sideways ? w : h, type);
        java.awt.Graphics2D g = out.createGraphics();
        switch (turn) {
            case 90 -> g.translate(h, 0);
            case 180 -> g.translate(w, h);
            default -> g.translate(0, w);
        }
        g.rotate(Math.toRadians(turn));
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return out;
    }

    private static BufferedImage scaled(BufferedImage image, double factor) {
        int tw = Math.max(1, (int) Math.round(image.getWidth() * factor));
        int th = Math.max(1, (int) Math.round(image.getHeight() * factor));
        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, tw, th, null);
        g.dispose();
        return out;
    }

    /** Writes an image for Tesseract to read from disk. */
    public static Path writePageImage(BufferedImage image, Path target) throws IOException {
        File file = target.toFile();
        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("No PNG writer available");
        }
        return target;
    }
}
