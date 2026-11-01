package com.docsaas.api.processing;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.contentstream.operator.text.BeginText;
import org.apache.pdfbox.contentstream.operator.text.EndText;
import org.apache.pdfbox.contentstream.operator.text.MoveText;
import org.apache.pdfbox.contentstream.operator.text.MoveTextSetLeading;
import org.apache.pdfbox.contentstream.operator.text.NextLine;
import org.apache.pdfbox.contentstream.operator.text.SetCharSpacing;
import org.apache.pdfbox.contentstream.operator.text.SetFontAndSize;
import org.apache.pdfbox.contentstream.operator.text.SetTextHorizontalScaling;
import org.apache.pdfbox.contentstream.operator.text.SetTextLeading;
import org.apache.pdfbox.contentstream.operator.text.SetTextRenderingMode;
import org.apache.pdfbox.contentstream.operator.text.SetTextRise;
import org.apache.pdfbox.contentstream.operator.text.SetWordSpacing;
import org.apache.pdfbox.contentstream.operator.text.ShowText;
import org.apache.pdfbox.contentstream.operator.text.ShowTextAdjusted;
import org.apache.pdfbox.contentstream.operator.text.ShowTextLine;
import org.apache.pdfbox.contentstream.operator.text.ShowTextLineAndSpace;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.state.PDTextState;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

/**
 * Removes the text under given areas of a page from its content stream, so erased or replaced
 * text is really gone (copy, search and text extraction no longer find it), not just covered.
 *
 * Each glyph whose center lies in an area is cut out of its show-text operator and replaced by an
 * equal TJ spacing adjustment, so the remaining glyphs of the line keep their exact positions.
 * Only the page's own content streams are rewritten: text inside form XObjects is left as is
 * (it stays covered by the whiteout drawn on top).
 */
final class TextEraser extends PDFStreamEngine {

    /** A glyph to remove: the show-text operator it belongs to and its index within that operator. */
    private record Hit(int operatorIndex, int glyphIndex) {
    }

    private final List<Rectangle2D> areas;
    private final Set<Hit> hits = new HashSet<>();
    private int operatorIndex = -1;
    private int glyphIndex;
    private int formDepth;

    private TextEraser(List<Rectangle2D> areas) {
        this.areas = areas;
        addOperator(new BeginText(this));
        addOperator(new EndText(this));
        addOperator(new SetFontAndSize(this));
        addOperator(new ShowText(this));
        addOperator(new ShowTextAdjusted(this));
        addOperator(new ShowTextLine(this));
        addOperator(new ShowTextLineAndSpace(this));
        addOperator(new MoveText(this));
        addOperator(new MoveTextSetLeading(this));
        addOperator(new NextLine(this));
        addOperator(new SetCharSpacing(this));
        addOperator(new SetTextHorizontalScaling(this));
        addOperator(new SetTextLeading(this));
        addOperator(new SetTextRenderingMode(this));
        addOperator(new SetTextRise(this));
        addOperator(new SetWordSpacing(this));
        addOperator(new SetMatrix(this));
        addOperator(new Concatenate(this));
        addOperator(new Save(this));
        addOperator(new Restore(this));
        addOperator(new SetGraphicsStateParameters(this));
    }

    /**
     * Erases text under the areas (in the page's own user space). Returns the number of glyphs
     * removed; the page's content is only rewritten when something was removed.
     */
    static int erase(org.apache.pdfbox.pdmodel.PDDocument doc, PDPage page, List<Rectangle2D> areas) throws IOException {
        if (areas.isEmpty() || !page.hasContents()) {
            return 0;
        }
        TextEraser eraser = new TextEraser(areas);
        eraser.processPage(page);
        if (eraser.hits.isEmpty()) {
            return 0;
        }
        eraser.rewrite(doc, page);
        return eraser.hits.size();
    }

    // ===== pass 1: find the glyphs =====

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        // only count operators of the page's own streams, in the order they are written
        if (formDepth == 0) {
            operatorIndex++;
            glyphIndex = 0;
        }
        if (OperatorName.DRAW_OBJECT.equals(operator.getName())) {
            formDepth++;
            try {
                super.processOperator(operator, operands);
            } finally {
                formDepth--;
            }
            return;
        }
        super.processOperator(operator, operands);
    }

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        if (formDepth == 0) {
            // glyph box in glyph space: advance width x (descent .. ascent), roughly -0.2 .. 0.8 em
            float w = displacement.getX();
            Point2D.Float center = textRenderingMatrix.transformPoint(w / 2, 0.3f);
            for (Rectangle2D area : areas) {
                if (area.contains(center)) {
                    hits.add(new Hit(operatorIndex, glyphIndex));
                    break;
                }
            }
        }
        glyphIndex++;
        super.showGlyph(textRenderingMatrix, font, code, displacement);
    }

    // ===== pass 2: rewrite the operators =====

    private void rewrite(org.apache.pdfbox.pdmodel.PDDocument doc, PDPage page) throws IOException {
        List<Object> tokens = new ArrayList<>();
        for (var it = page.getContentStreams(); it.hasNext();) {
            PDFStreamParser parser = new PDFStreamParser(it.next().toByteArray());
            for (Object token = parser.parseNextToken(); token != null; token = parser.parseNextToken()) {
                tokens.add(token);
            }
        }

        // replay the text state alongside, to size the gaps left by removed glyphs
        Map<Integer, List<Integer>> hitsByOperator = new HashMap<>();
        hits.forEach(h -> hitsByOperator.computeIfAbsent(h.operatorIndex(), k -> new ArrayList<>()).add(h.glyphIndex()));

        TextReplay state = new TextReplay(page);
        List<Object> out = new ArrayList<>(tokens.size());
        List<COSBase> operands = new ArrayList<>();
        int index = -1;
        for (Object token : tokens) {
            if (!(token instanceof Operator op)) {
                operands.add((COSBase) token);
                continue;
            }
            index++;
            state.apply(op, operands);
            List<Integer> remove = hitsByOperator.get(index);
            if (remove != null && isShowText(op.getName())) {
                out.addAll(cutGlyphs(op, operands, new HashSet<>(remove), state));
            } else {
                out.addAll(operands);
                out.add(op);
            }
            operands = new ArrayList<>();
        }

        PDStream stream = new PDStream(doc);
        try (OutputStream os = stream.createOutputStream(COSName.FLATE_DECODE)) {
            new ContentStreamWriter(os).writeTokens(out);
        }
        page.setContents(stream);
    }

    private static boolean isShowText(String name) {
        return name.equals(OperatorName.SHOW_TEXT) || name.equals(OperatorName.SHOW_TEXT_ADJUSTED)
                || name.equals(OperatorName.SHOW_TEXT_LINE) || name.equals(OperatorName.SHOW_TEXT_LINE_AND_SPACE);
    }

    /**
     * Rewrites one show-text operator as a TJ array without the removed glyphs. ' and " first move
     * to the next line (and " sets spacing), exactly as they would have.
     */
    private static List<Object> cutGlyphs(Operator op, List<COSBase> operands, Set<Integer> remove, TextReplay state)
            throws IOException {
        List<Object> out = new ArrayList<>();
        COSBase textOperand;
        switch (op.getName()) {
            case OperatorName.SHOW_TEXT_LINE -> {
                out.add(Operator.getOperator(OperatorName.NEXT_LINE));
                textOperand = operands.get(0);
            }
            case OperatorName.SHOW_TEXT_LINE_AND_SPACE -> {
                out.add(operands.get(0));
                out.add(Operator.getOperator(OperatorName.SET_WORD_SPACING));
                out.add(operands.get(1));
                out.add(Operator.getOperator(OperatorName.SET_CHAR_SPACING));
                out.add(Operator.getOperator(OperatorName.NEXT_LINE));
                textOperand = operands.get(2);
            }
            default -> textOperand = operands.isEmpty() ? null : operands.get(0);
        }

        COSArray items = new COSArray();
        if (textOperand instanceof COSString s) {
            items.add(s);
        } else if (textOperand instanceof COSArray a) {
            a.forEach(items::add);
        } else {
            out.addAll(operands);
            out.add(op);
            return out;
        }

        PDFont font = state.font();
        float size = state.text().getFontSize();
        float charSpacing = state.text().getCharacterSpacing();
        float wordSpacing = state.text().getWordSpacing();
        COSArray result = new COSArray();
        int glyph = 0;
        for (COSBase item : items) {
            if (!(item instanceof COSString str) || font == null) {
                result.add(item);
                continue;
            }
            byte[] bytes = str.getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream kept = new ByteArrayOutputStream();
            while (in.available() > 0) {
                int before = bytes.length - in.available();
                int code = font.readCode(in);
                int length = bytes.length - in.available() - before;
                if (remove.contains(glyph)) {
                    if (kept.size() > 0) {
                        result.add(new COSString(kept.toByteArray()));
                        kept.reset();
                    }
                    // same advance as the glyph: width plus character (and word) spacing, in TJ units
                    float advance = font.getWidth(code)
                            + (size == 0 ? 0 : (charSpacing + (length == 1 && code == 32 ? wordSpacing : 0)) * 1000f / size);
                    result.add(new COSFloat(-advance));
                } else {
                    kept.write(bytes, before, length);
                }
                glyph++;
            }
            if (kept.size() > 0) {
                result.add(new COSString(kept.toByteArray()));
            }
        }
        out.add(result);
        out.add(Operator.getOperator(OperatorName.SHOW_TEXT_ADJUSTED));
        return out;
    }

    /** Minimal replay of the text state (font, size, spacing) while walking the tokens. */
    private static final class TextReplay {
        private final PDPage page;
        private final java.util.ArrayDeque<Object[]> stack = new java.util.ArrayDeque<>();
        private PDFont font;
        private final PDTextState text = new PDTextState();

        TextReplay(PDPage page) {
            this.page = page;
        }

        PDFont font() {
            return font;
        }

        PDTextState text() {
            return text;
        }

        void apply(Operator op, List<COSBase> operands) throws IOException {
            switch (op.getName()) {
                case OperatorName.SAVE -> stack.push(new Object[] { font, text.getFontSize(), text.getCharacterSpacing(), text.getWordSpacing() });
                case OperatorName.RESTORE -> {
                    Object[] s = stack.poll();
                    if (s != null) {
                        font = (PDFont) s[0];
                        text.setFontSize((Float) s[1]);
                        text.setCharacterSpacing((Float) s[2]);
                        text.setWordSpacing((Float) s[3]);
                    }
                }
                case OperatorName.SET_FONT_AND_SIZE -> {
                    if (operands.size() >= 2 && operands.get(0) instanceof COSName name && operands.get(1) instanceof COSNumber n) {
                        font = page.getResources() == null ? null : page.getResources().getFont(name);
                        text.setFontSize(n.floatValue());
                    }
                }
                case OperatorName.SET_CHAR_SPACING -> {
                    if (!operands.isEmpty() && operands.get(0) instanceof COSNumber n) {
                        text.setCharacterSpacing(n.floatValue());
                    }
                }
                case OperatorName.SET_WORD_SPACING -> {
                    if (!operands.isEmpty() && operands.get(0) instanceof COSNumber n) {
                        text.setWordSpacing(n.floatValue());
                    }
                }
                case OperatorName.SHOW_TEXT_LINE_AND_SPACE -> {
                    if (operands.size() >= 2 && operands.get(0) instanceof COSNumber aw && operands.get(1) instanceof COSNumber ac) {
                        text.setWordSpacing(aw.floatValue());
                        text.setCharacterSpacing(ac.floatValue());
                    }
                }
                case OperatorName.SET_GRAPHICS_STATE_PARAMS -> {
                    if (!operands.isEmpty() && operands.get(0) instanceof COSName name && page.getResources() != null) {
                        var gs = page.getResources().getExtGState(name);
                        if (gs != null && gs.getFontSetting() != null) {
                            font = gs.getFontSetting().getFont();
                            text.setFontSize(gs.getFontSetting().getFontSize());
                        }
                    }
                }
                default -> {
                }
            }
        }
    }
}
