package com.docsaas.api.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.processing.engine.AiClient;
import com.docsaas.api.processing.engine.AiClient.AiException;
import com.docsaas.api.processing.engine.AiClient.Message;
import com.docsaas.api.processing.engine.DocumentIndex;
import com.docsaas.api.processing.engine.PdfTextExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Structured insights from a PDF: overview, document type, topics, key points, dates, people and
 * organizations, figures and action items (SRS FR-018).
 *
 * Every extracted item carries a page and a short verbatim quote. The quote is checked against the
 * text of that page: items whose quote is found are marked verified, the rest are kept but flagged,
 * so uncertain extraction is never presented as fact. Long documents are processed in parts and
 * the results merged.
 */
@Component
public class DocumentInsightsProcessor implements ToolProcessor {

    /** List name -> maximum items kept. */
    static final Map<String, Integer> LISTS = new LinkedHashMap<>();

    static {
        LISTS.put("keyPoints", 10);
        LISTS.put("dates", 20);
        LISTS.put("entities", 25);
        LISTS.put("figures", 15);
        LISTS.put("actionItems", 15);
    }

    private static final Set<String> ENTITY_TYPES = Set.of("person", "organization", "place", "product", "other");
    private static final int MAX_TOPICS = 8;
    private static final int MAX_TEXT = 300;
    private static final int MAX_QUOTE = 200;

    private static final String SYSTEM = """
            You extract structured insights from documents.
            Rules:
            - Use only information that appears in the provided document text. Never invent facts, names, numbers or dates.
            - For every item give the page number from the [Page N] markers and "quote": a short verbatim excerpt (5-20 words) copied exactly from that page that supports the item.
            - Leave a list empty when the document has nothing for it.
            - Write values in the same language as the document.
            - Reply with one JSON object only, no Markdown and no commentary.""";

    private static final String SCHEMA = """
            Return JSON with exactly these fields:
            {
              "overview": "2-3 sentence plain-language overview",
              "documentType": "short label, e.g. invoice, contract, research paper, resume, report, letter",
              "topics": ["up to 8 short topic labels"],
              "keyPoints": [{"text": "...", "page": 1, "quote": "..."}],
              "dates": [{"date": "as written in the document", "description": "what happens on this date", "page": 1, "quote": "..."}],
              "entities": [{"name": "...", "type": "person|organization|place|product|other", "role": "short role in the document", "page": 1, "quote": "..."}],
              "figures": [{"value": "amount or number with unit", "description": "what it measures", "page": 1, "quote": "..."}],
              "actionItems": [{"text": "what must be done", "owner": "who, if stated", "due": "deadline, if stated", "page": 1, "quote": "..."}]
            }""";

    private final AiClient ai;
    private final PdfTextExtractor extractor;
    private final ObjectMapper objectMapper;

    @Value("${docsaas.ai.max-pages:100}")
    private int maxPages;

    @Value("${docsaas.ai.max-input-chars:120000}")
    private int maxInputChars;

    public DocumentInsightsProcessor(AiClient ai, PdfTextExtractor extractor, ObjectMapper objectMapper) {
        this.ai = ai;
        this.extractor = extractor;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolCode tool() {
        return ToolCode.DOCUMENT_INSIGHTS;
    }

    @Override
    public String unavailableReason() {
        return ai.unavailableReason();
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx) throws IOException {
        ProcessingContext.InputFile input = ctx.inputs().get(0);

        List<PdfTextExtractor.PageText> pages;
        int totalPages;
        PDDocument doc = PdfSupport.load(input);
        try {
            totalPages = doc.getNumberOfPages();
            pages = extractor.extract(doc, maxPages);
        } finally {
            PdfSupport.close(doc);
        }
        DocumentIndex index = DocumentIndex.build(pages);
        if (index.totalChars() < 40) {
            throw new ProcessingException("NO_TEXT_FOUND",
                    "No readable text was found in this PDF. If it is scanned, try Scanned PDF OCR first.");
        }

        List<List<DocumentIndex.Chunk>> parts = split(index);
        List<JsonNode> extracted = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            String intro = parts.size() == 1 ? "" : "This is part " + (i + 1) + " of " + parts.size() + " of a long document.\n";
            extracted.add(ask(intro + SCHEMA + "\n\nDocument:\n\n" + DocumentIndex.asExcerpts(parts.get(i))));
        }

        Map<Integer, String> pageText = new HashMap<>();
        pages.forEach(p -> pageText.put(p.page(), normalize(p.text())));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("document", input.originalName());
        result.put("pages", totalPages);
        result.put("pagesAnalyzed", index.pages());
        result.put("overview", overview(extracted));
        result.put("documentType", clean(extracted.get(0).path("documentType").asText(""), 60));
        result.put("topics", topics(extracted));
        int verified = 0;
        int total = 0;
        for (Map.Entry<String, Integer> list : LISTS.entrySet()) {
            List<Map<String, Object>> items = mergeItems(extracted, list.getKey(), list.getValue(), pageText, index.pages());
            result.put(list.getKey(), items);
            total += items.size();
            verified += (int) items.stream().filter(it -> Boolean.TRUE.equals(it.get("verified"))).count();
        }
        result.put("generatedBy", "AI");
        result.put("notice", "AI-generated insights. Items marked unverified could not be matched to the quoted text "
                + "on the cited page; check them against the original.");

        Path out = ctx.workDir().resolve("insights.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), result);

        String note = index.pages() < totalPages ? " Only the first " + index.pages() + " pages were analyzed." : "";
        return new ProcessingResult(out, FileNames.baseName(input.originalName()) + "_insights.json",
                "application/json", total + " insights found, " + verified + " verified against the text." + note);
    }

    // ===== AI calls =====

    private JsonNode ask(String prompt) {
        String reply = ai.complete(List.of(Message.system(SYSTEM), Message.user(prompt)), 4096);
        try {
            JsonNode node = objectMapper.readTree(jsonPart(reply));
            if (node != null && node.isObject()) {
                return node;
            }
        } catch (JsonProcessingException ignored) {
            // handled below
        }
        // Models occasionally return malformed JSON; a retry usually fixes it.
        throw new AiException("The AI service returned an unreadable answer. Please try again.", true);
    }

    private String overview(List<JsonNode> extracted) {
        if (extracted.size() == 1) {
            return clean(extracted.get(0).path("overview").asText(""), 1200);
        }
        StringBuilder parts = new StringBuilder();
        for (int i = 0; i < extracted.size(); i++) {
            parts.append("Part ").append(i + 1).append(": ").append(extracted.get(i).path("overview").asText("")).append("\n");
        }
        String combined = ai.complete(List.of(
                Message.system("You write short, factual overviews. Use only the information given. Reply with plain text only."),
                Message.user("These are overviews of consecutive parts of one document. Write one 2-3 sentence overview "
                        + "of the whole document.\n\n" + parts)), 400);
        return clean(combined, 1200);
    }

    // ===== merging and verification =====

    private List<String> topics(List<JsonNode> extracted) {
        Set<String> seen = new HashSet<>();
        List<String> topics = new ArrayList<>();
        for (JsonNode part : extracted) {
            for (JsonNode t : part.path("topics")) {
                String topic = clean(t.asText(""), 60);
                if (!topic.isEmpty() && seen.add(topic.toLowerCase(Locale.ROOT)) && topics.size() < MAX_TOPICS) {
                    topics.add(topic);
                }
            }
        }
        return topics;
    }

    List<Map<String, Object>> mergeItems(List<JsonNode> extracted, String list, int max,
            Map<Integer, String> pageText, int pagesAnalyzed) {
        String keyField = switch (list) {
            case "dates" -> "date";
            case "entities" -> "name";
            case "figures" -> "value";
            default -> "text";
        };
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonNode part : extracted) {
            for (JsonNode raw : part.path(list)) {
                if (items.size() >= max || !raw.isObject()) {
                    continue;
                }
                String key = clean(raw.path(keyField).asText(""), MAX_TEXT);
                if (key.isEmpty() || !seen.add(list.equals("keyPoints") || list.equals("actionItems")
                        ? normalize(key) : normalize(key) + "|" + normalize(raw.path("description").asText("")))) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put(keyField, key);
                switch (list) {
                    case "dates", "figures" -> item.put("description", clean(raw.path("description").asText(""), MAX_TEXT));
                    case "entities" -> {
                        String type = raw.path("type").asText("other").toLowerCase(Locale.ROOT);
                        item.put("type", ENTITY_TYPES.contains(type) ? type : "other");
                        item.put("role", clean(raw.path("role").asText(""), 120));
                    }
                    case "actionItems" -> {
                        item.put("owner", clean(raw.path("owner").asText(""), 120));
                        item.put("due", clean(raw.path("due").asText(""), 80));
                    }
                    default -> {
                    }
                }
                String quote = clean(raw.path("quote").asText(""), MAX_QUOTE);
                int page = raw.path("page").asInt(0);
                Integer foundOn = locate(quote, page, pageText);
                // A page outside the analyzed range is never trusted; a quote found elsewhere fixes the page.
                item.put("page", foundOn != null ? foundOn : (page >= 1 && page <= pagesAnalyzed ? page : null));
                item.put("quote", quote);
                item.put("verified", foundOn != null);
                items.add(item);
            }
        }
        return items;
    }

    /** Page where the quote appears (cited page first), or null if it appears nowhere. */
    static Integer locate(String quote, int citedPage, Map<Integer, String> pageText) {
        String q = normalize(quote);
        if (q.length() < 8) {
            return null;
        }
        if (pageText.getOrDefault(citedPage, "").contains(q)) {
            return citedPage;
        }
        return pageText.entrySet().stream()
                .filter(e -> e.getValue().contains(q))
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst()
                .orElse(null);
    }

    /** Lowercase letters and digits separated by single spaces, so layout and punctuation differences don't matter. */
    static String normalize(String text) {
        String s = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return s.replaceAll("[^\\p{L}\\p{N}\\p{M}]+", " ").strip();
    }

    /** The JSON object in a reply, tolerating Markdown fences or text around it. */
    static String jsonPart(String reply) {
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        return start >= 0 && end > start ? reply.substring(start, end + 1) : reply;
    }

    private static String clean(String s, int max) {
        String v = s == null ? "" : s.replaceAll("\\s+", " ").strip();
        return v.length() <= max ? v : v.substring(0, max).strip() + "…";
    }

    private List<List<DocumentIndex.Chunk>> split(DocumentIndex index) {
        List<List<DocumentIndex.Chunk>> parts = new ArrayList<>();
        List<DocumentIndex.Chunk> current = new ArrayList<>();
        int used = 0;
        int budget = Math.max(20_000, maxInputChars / 2);
        for (DocumentIndex.Chunk c : index.chunks()) {
            if (used + c.text().length() > budget && !current.isEmpty()) {
                parts.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(c);
            used += c.text().length();
        }
        if (!current.isEmpty()) {
            parts.add(current);
        }
        return parts;
    }
}
