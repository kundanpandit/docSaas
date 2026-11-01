package com.docsaas.api.processing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fonts for text added in the PDF editor. Latin text uses the standard PDF fonts (Helvetica,
 * Times, Courier: not embedded, metric-compatible with Arial, Times New Roman and Courier New that
 * the editor shows). Text the standard fonts can't encode (Hindi, other scripts) uses the first
 * installed Unicode font that has every character, embedded as a subset.
 */
@Component
public class EditorFonts {

    private static final Logger log = LoggerFactory.getLogger(EditorFonts.class);

    /** Font files tried in order for non-Latin text (.ttf or .ttc). Missing files are skipped. */
    @Value("${docsaas.fonts.unicode:C:/Windows/Fonts/Nirmala.ttc,C:/Windows/Fonts/Nirmala.ttf,"
            + "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf,"
            + "/usr/share/fonts/truetype/noto/NotoSansDevanagari-Regular.ttf,"
            + "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf}")
    private String unicodeFonts;

    public ForDocument forDocument(PDDocument doc) {
        return new ForDocument(doc, Arrays.stream(unicodeFonts.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(File::new).filter(File::isFile).toList());
    }

    /** Fonts for one output document; loads Unicode fonts on first use. Close after saving. */
    public static final class ForDocument implements AutoCloseable {

        private final PDDocument doc;
        private final List<File> candidates;
        private List<PDFont> unicode;
        private final List<AutoCloseable> open = new ArrayList<>();

        ForDocument(PDDocument doc, List<File> candidates) {
            this.doc = doc;
            this.candidates = candidates;
        }

        public static PDFont standard(String family, boolean bold, boolean italic) {
            FontName name = switch (family) {
                case "times" -> bold ? (italic ? FontName.TIMES_BOLD_ITALIC : FontName.TIMES_BOLD)
                        : (italic ? FontName.TIMES_ITALIC : FontName.TIMES_ROMAN);
                case "courier" -> bold ? (italic ? FontName.COURIER_BOLD_OBLIQUE : FontName.COURIER_BOLD)
                        : (italic ? FontName.COURIER_OBLIQUE : FontName.COURIER);
                default -> bold ? (italic ? FontName.HELVETICA_BOLD_OBLIQUE : FontName.HELVETICA_BOLD)
                        : (italic ? FontName.HELVETICA_OBLIQUE : FontName.HELVETICA);
            };
            return new PDType1Font(name);
        }

        /** A Unicode font that can draw every line, or null if none is installed. */
        public PDFont unicodeFor(List<String> lines) {
            if (unicode == null) {
                unicode = new ArrayList<>();
                for (File file : candidates) {
                    try {
                        load(file);
                    } catch (IOException | RuntimeException e) {
                        log.warn("Could not load font {}", file.getName());
                    }
                }
            }
            for (PDFont font : unicode) {
                if (lines.stream().allMatch(line -> canEncode(font, line))) {
                    return font;
                }
            }
            return null;
        }

        private void load(File file) throws IOException {
            if (file.getName().toLowerCase().endsWith(".ttc")) {
                TrueTypeCollection collection = new TrueTypeCollection(file);
                open.add(collection);
                List<TrueTypeFont> fonts = new ArrayList<>();
                collection.processAllFonts(fonts::add);
                // Prefer the regular face (collections also hold bold/light variants)
                TrueTypeFont regular = fonts.stream()
                        .filter(f -> !nameOf(f).matches("(?i).*(bold|light|semi|black|italic).*"))
                        .findFirst().orElse(fonts.isEmpty() ? null : fonts.get(0));
                if (regular != null) {
                    unicode.add(PDType0Font.load(doc, regular, true));
                }
            } else {
                unicode.add(PDType0Font.load(doc, file));
            }
        }

        private static String nameOf(TrueTypeFont font) {
            try {
                return font.getName();
            } catch (IOException e) {
                return "";
            }
        }

        public static boolean canEncode(PDFont font, String text) {
            try {
                font.encode(text);
                return true;
            } catch (IOException | IllegalArgumentException e) {
                return false;
            }
        }

        @Override
        public void close() {
            for (AutoCloseable c : open) {
                try {
                    c.close();
                } catch (Exception ignored) {
                    // nothing useful to do
                }
            }
        }
    }
}
