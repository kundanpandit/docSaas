package com.docsaas.api.processing;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;

/**
 * Converts PDF pages to an Excel workbook, one sheet per page, rebuilding the table layout from
 * text positions: glyphs on the same baseline form a row, large horizontal gaps split a row into
 * cells, and columns are the horizontal bands where cells of multi-cell rows overlap (so left-,
 * right- and center-aligned columns all work).
 * Works well on digital PDFs with simple tables; scanned pages have no text and are skipped.
 * Numbers such as "1,250.50" or "-12" become numeric cells.
 */
@Component
public class PdfToExcelProcessor implements ToolProcessor {

    static final int MAX_PAGES = 200;
    private static final Pattern NUMBER = Pattern.compile("-?\\(?\\d{1,3}(,\\d{3})*(\\.\\d+)?\\)?|-?\\d+(\\.\\d+)?");

    record Segment(float x, float xEnd, float y, String text) {
    }

    @Override
    public ToolCode tool() {
        return ToolCode.PDF_TO_EXCEL;
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);
        Path out = ctx.workDir().resolve("tables.xlsx");

        int pages;
        int converted = 0;
        int empty = 0;
        PDDocument doc = PdfSupport.load(input);
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            CellStyle numberStyle = book.createCellStyle();
            numberStyle.setDataFormat(book.createDataFormat().getFormat("#,##0.##"));
            pages = Math.min(doc.getNumberOfPages(), MAX_PAGES);
            for (int p = 1; p <= pages; p++) {
                List<List<String>> table = layout(segments(doc, p));
                if (table.isEmpty()) {
                    empty++;
                    continue;
                }
                writeSheet(book.createSheet("Page " + p), table, numberStyle);
                converted++;
            }
            if (converted == 0) {
                throw new ProcessingException("NO_TEXT_FOUND",
                        "No text was found in this PDF. If it is scanned, run Scanned PDF OCR first, then convert the result.");
            }
            try (OutputStream os = Files.newOutputStream(out)) {
                book.write(os);
            }
        } finally {
            PdfSupport.close(doc);
        }

        StringBuilder summary = new StringBuilder(converted + (converted == 1 ? " page" : " pages") + " converted to Excel sheets.");
        if (empty > 0) {
            summary.append(" ").append(empty).append(empty == 1 ? " page had" : " pages had").append(" no text and were skipped.");
        }
        if (doc.getNumberOfPages() > MAX_PAGES) {
            summary.append(" Only the first ").append(MAX_PAGES).append(" pages were converted.");
        }
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", summary.toString());
    }

    /** Text segments of one page: runs of glyphs on one line without a column-sized gap. */
    static List<Segment> segments(PDDocument doc, int page) throws IOException {
        List<TextPosition> glyphs = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition text) {
                if (!text.getUnicode().isBlank()) {
                    glyphs.add(text);
                }
            }
        };
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.getText(doc);

        // Group into lines by baseline (tolerance: 40% of the font height)
        glyphs.sort(Comparator.comparingDouble(TextPosition::getYDirAdj).thenComparingDouble(TextPosition::getXDirAdj));
        List<List<TextPosition>> lines = new ArrayList<>();
        for (TextPosition g : glyphs) {
            List<TextPosition> line = lines.isEmpty() ? null : lines.get(lines.size() - 1);
            if (line != null && Math.abs(line.get(0).getYDirAdj() - g.getYDirAdj()) <= Math.max(2, g.getHeightDir() * 0.4)) {
                line.add(g);
            } else {
                line = new ArrayList<>();
                line.add(g);
                lines.add(line);
            }
        }

        List<Segment> segments = new ArrayList<>();
        for (List<TextPosition> line : lines) {
            line.sort(Comparator.comparingDouble(TextPosition::getXDirAdj));
            StringBuilder text = new StringBuilder();
            float startX = line.get(0).getXDirAdj();
            float prevEnd = startX;
            for (TextPosition g : line) {
                float gap = g.getXDirAdj() - prevEnd;
                float fontSize = Math.max(4, g.getFontSizeInPt());
                if (text.length() > 0 && gap > fontSize * 1.2f) {
                    segments.add(new Segment(startX, prevEnd, line.get(0).getYDirAdj(), text.toString().strip()));
                    text.setLength(0);
                    startX = g.getXDirAdj();
                } else if (text.length() > 0 && gap > fontSize * 0.15f) {
                    text.append(' ');
                }
                text.append(g.getUnicode());
                prevEnd = g.getXDirAdj() + g.getWidthDirAdj();
            }
            segments.add(new Segment(startX, prevEnd, line.get(0).getYDirAdj(), text.toString().strip()));
        }
        return segments;
    }

    /**
     * Rows of cells: segments on the same line are one row. Columns are built only from rows with
     * two or more cells (table rows), by merging overlapping horizontal extents; single-cell lines
     * such as titles and paragraphs would otherwise glue every column together.
     */
    static List<List<String>> layout(List<Segment> segments) {
        if (segments.isEmpty()) {
            return List.of();
        }
        java.util.Map<Float, Long> cellsPerLine = segments.stream()
                .collect(java.util.stream.Collectors.groupingBy(Segment::y, java.util.stream.Collectors.counting()));
        List<float[]> bands = new ArrayList<>();
        segments.stream()
                .filter(s -> cellsPerLine.get(s.y()) > 1)
                .sorted(Comparator.comparingDouble(Segment::x))
                .forEach(s -> {
                    float[] last = bands.isEmpty() ? null : bands.get(bands.size() - 1);
                    if (last != null && s.x() <= last[1] + 2) {
                        last[1] = Math.max(last[1], s.xEnd());
                    } else {
                        bands.add(new float[] { s.x(), s.xEnd() });
                    }
                });
        List<Float> columns = bands.stream().map(b -> b[0]).toList();

        List<List<String>> rows = new ArrayList<>();
        float currentY = Float.NaN;
        List<String> row = null;
        for (Segment s : segments) {
            if (row == null || s.y() != currentY) {
                row = new ArrayList<>();
                rows.add(row);
                currentY = s.y();
            }
            int col = 0;
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i) <= s.x() + 2) {
                    col = i;
                }
            }
            while (row.size() <= col) {
                row.add("");
            }
            row.set(col, row.get(col).isEmpty() ? s.text() : row.get(col) + " " + s.text());
        }
        return rows;
    }

    private static void writeSheet(Sheet sheet, List<List<String>> table, CellStyle numberStyle) {
        int[] widths = new int[table.stream().mapToInt(List::size).max().orElse(0)];
        for (int r = 0; r < table.size(); r++) {
            Row row = sheet.createRow(r);
            List<String> cells = table.get(r);
            for (int c = 0; c < cells.size(); c++) {
                String value = cells.get(c);
                if (value.isEmpty()) {
                    continue;
                }
                Cell cell = row.createCell(c);
                Double number = parseNumber(value);
                if (number != null) {
                    cell.setCellValue(number);
                    cell.setCellStyle(numberStyle);
                } else {
                    cell.setCellValue(value);
                }
                widths[c] = Math.max(widths[c], Math.min(value.length(), 60));
            }
        }
        for (int c = 0; c < widths.length; c++) {
            sheet.setColumnWidth(c, Math.max(8, widths[c] + 2) * 256);
        }
    }

    /** "1,250.50" -> 1250.5, "(12)" -> -12 (accounting negative); anything else -> null. */
    static Double parseNumber(String value) {
        String v = value.strip();
        if (!NUMBER.matcher(v).matches()) {
            return null;
        }
        boolean parens = v.startsWith("(") || v.startsWith("-(");
        if (parens != v.endsWith(")")) {
            return null;
        }
        String digits = v.replace(",", "").replace("(", "").replace(")", "");
        // Codes and IDs (leading zeros, phone or account numbers) must stay text
        String plain = digits.replace("-", "");
        if ((plain.length() > 1 && plain.startsWith("0") && !plain.startsWith("0.")) || plain.replace(".", "").length() > 15) {
            return null;
        }
        try {
            double d = Double.parseDouble(digits);
            return parens ? -Math.abs(d) : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
