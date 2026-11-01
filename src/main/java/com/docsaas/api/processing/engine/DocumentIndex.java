package com.docsaas.api.processing.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Page-tagged text chunks plus a small BM25 keyword ranker. Free and dependency-less:
 * good enough to pick the relevant passages of a single document for the AI to read.
 */
public record DocumentIndex(int pages, List<Chunk> chunks) {

    public record Chunk(int page, String text) {
    }

    public static final int CHUNK_CHARS = 1200;

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "is", "are", "was", "were", "be",
            "it", "this", "that", "with", "as", "by", "at", "from", "what", "which", "who", "how", "does",
            "do", "did", "can", "about", "please", "tell", "me", "document", "pdf", "file");

    /** Splits each page into chunks of at most ~CHUNK_CHARS, breaking at paragraph or sentence ends. */
    public static DocumentIndex build(List<PdfTextExtractor.PageText> pages) {
        List<Chunk> chunks = new ArrayList<>();
        for (PdfTextExtractor.PageText page : pages) {
            String text = page.text();
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(text.length(), start + CHUNK_CHARS);
                if (end < text.length()) {
                    int cut = Math.max(text.lastIndexOf("\n\n", end), text.lastIndexOf(". ", end));
                    if (cut > start + CHUNK_CHARS / 2) {
                        end = cut + 1;
                    }
                }
                String piece = text.substring(start, end).strip();
                if (!piece.isEmpty()) {
                    chunks.add(new Chunk(page.page(), piece));
                }
                start = end;
            }
        }
        return new DocumentIndex(pages.size(), chunks);
    }

    public int totalChars() {
        return chunks.stream().mapToInt(c -> c.text().length()).sum();
    }

    /** Best-matching chunks for the query, up to {@code charBudget}, returned in document order. */
    public List<Chunk> search(String query, int charBudget) {
        List<String> terms = tokenize(query);
        if (terms.isEmpty() || chunks.isEmpty()) {
            return firstChunks(charBudget);
        }

        List<List<String>> docs = chunks.stream().map(c -> tokenize(c.text())).toList();
        double avgLen = docs.stream().mapToInt(List::size).average().orElse(1);
        Map<String, Integer> df = new HashMap<>();
        for (List<String> d : docs) {
            new HashSet<>(d).forEach(t -> df.merge(t, 1, Integer::sum));
        }

        double k1 = 1.4;
        double b = 0.75;
        int n = docs.size();
        double[] scores = new double[n];
        for (int i = 0; i < n; i++) {
            Map<String, Integer> tf = new HashMap<>();
            docs.get(i).forEach(t -> tf.merge(t, 1, Integer::sum));
            for (String term : terms) {
                int f = tf.getOrDefault(term, 0);
                if (f == 0) {
                    continue;
                }
                double idf = Math.log(1 + (n - df.get(term) + 0.5) / (df.get(term) + 0.5));
                scores[i] += idf * (f * (k1 + 1)) / (f + k1 * (1 - b + b * docs.get(i).size() / avgLen));
            }
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble((Integer i) -> scores[i]).reversed());

        if (scores[order.get(0)] == 0) {
            // Nothing matched (e.g. "summarize this"): give the model the start of the document.
            return firstChunks(charBudget);
        }

        List<Integer> picked = new ArrayList<>();
        int used = 0;
        for (int i : order) {
            if (scores[i] == 0 || used + chunks.get(i).text().length() > charBudget) {
                continue;
            }
            picked.add(i);
            used += chunks.get(i).text().length();
        }
        picked.sort(Integer::compare);
        return picked.stream().map(chunks::get).toList();
    }

    private List<Chunk> firstChunks(int charBudget) {
        List<Chunk> out = new ArrayList<>();
        int used = 0;
        for (Chunk c : chunks) {
            if (used + c.text().length() > charBudget) {
                break;
            }
            out.add(c);
            used += c.text().length();
        }
        return out;
    }

    static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (raw.length() > 1 && !STOPWORDS.contains(raw)) {
                // crude stemming: plural "s"
                out.add(raw.length() > 4 && raw.endsWith("s") ? raw.substring(0, raw.length() - 1) : raw);
            }
        }
        return out;
    }

    /** Formats chunks as page-labelled excerpts for a prompt. */
    public static String asExcerpts(List<Chunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (Chunk c : chunks) {
            sb.append("[Page ").append(c.page()).append("]\n").append(c.text()).append("\n\n");
        }
        return sb.toString().strip();
    }
}
