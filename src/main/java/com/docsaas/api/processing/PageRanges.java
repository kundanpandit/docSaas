package com.docsaas.api.processing;

import java.util.ArrayList;
import java.util.List;

/** Parses user page ranges such as "1-3, 5, 8-10" (1-based, inclusive). */
public final class PageRanges {

    public static final int MAX_RANGES = 100;

    public record Range(int from, int to) {
        public String label() {
            return from == to ? String.valueOf(from) : from + "-" + to;
        }
    }

    private PageRanges() {
    }

    /** Syntax-only check, usable before the page count is known. */
    public static List<Range> parse(String text) {
        if (text == null || text.isBlank()) {
            throw new ProcessingException("INVALID_RANGES", "Please enter the pages to split, for example 1-3, 5.");
        }
        List<Range> ranges = new ArrayList<>();
        for (String part : text.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            String[] bounds = p.split("-", -1);
            try {
                int from;
                int to;
                if (bounds.length == 1) {
                    from = to = Integer.parseInt(bounds[0].trim());
                } else if (bounds.length == 2) {
                    from = Integer.parseInt(bounds[0].trim());
                    to = Integer.parseInt(bounds[1].trim());
                } else {
                    throw new NumberFormatException();
                }
                if (from < 1 || to < from) {
                    throw new ProcessingException("INVALID_RANGES",
                            "\"" + p + "\" is not a valid page range. Use ascending page numbers starting at 1.");
                }
                ranges.add(new Range(from, to));
            } catch (NumberFormatException e) {
                throw new ProcessingException("INVALID_RANGES",
                        "\"" + p + "\" is not a valid page range. Use numbers like 1-3, 5.");
            }
        }
        if (ranges.isEmpty()) {
            throw new ProcessingException("INVALID_RANGES", "Please enter the pages to split, for example 1-3, 5.");
        }
        if (ranges.size() > MAX_RANGES) {
            throw new ProcessingException("INVALID_RANGES", "Too many ranges (maximum " + MAX_RANGES + ").");
        }
        return ranges;
    }

    public static List<Range> parse(String text, int pageCount) {
        List<Range> ranges = parse(text);
        for (Range r : ranges) {
            if (r.to() > pageCount) {
                throw new ProcessingException("INVALID_RANGES",
                        "Page range " + r.label() + " is outside the document, which has " + pageCount + " pages.");
            }
        }
        return ranges;
    }
}
