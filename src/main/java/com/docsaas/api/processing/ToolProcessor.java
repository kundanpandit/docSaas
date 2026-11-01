package com.docsaas.api.processing;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.docsaas.api.enums.ToolCode;

/** One implementation per tool. Implementations must not log document contents. */
public interface ToolProcessor {

    ToolCode tool();

    /** Fast, synchronous option checks done before a job is accepted. Throw ProcessingException on bad input. */
    default void validateOptions(Map<String, String> options) {
    }

    /**
     * Largest JSON data part this tool accepts (sent as the multipart field "data"); 0 = none.
     * Options are short strings, so tools with structured input use this instead.
     */
    default long maxDataBytes() {
        return 0;
    }

    /** Fast checks of the data part (null when none was sent) before a job is accepted. */
    default void validateData(com.fasterxml.jackson.databind.JsonNode data) {
    }

    /** Options that must not be kept (e.g. passwords): removed from the stored job once it finishes. */
    default Set<String> sensitiveOptions() {
        return Set.of();
    }

    /**
     * Null when the tool can run; otherwise a user-safe reason (e.g. an engine is not installed
     * or not configured). Checked before a job is accepted and reported by GET /api/v1/tools.
     */
    default String unavailableReason() {
        return null;
    }

    ProcessingResult process(ProcessingContext context) throws IOException;
}
