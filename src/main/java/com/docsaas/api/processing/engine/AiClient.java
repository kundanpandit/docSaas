package com.docsaas.api.processing.engine;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.PostConstruct;

/**
 * Minimal client for any OpenAI-compatible chat completions API, so the AI provider is a
 * configuration choice (Gemini free tier, Groq, OpenRouter, a local Ollama, ...).
 * Never logs prompts or document text.
 */
@Service
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    public record Message(String role, String content) {
        public static Message system(String content) {
            return new Message("system", content);
        }

        public static Message user(String content) {
            return new Message("user", content);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content);
        }
    }

    /** {@code retryable} is true for rate limits, timeouts and provider outages. */
    public static class AiException extends RuntimeException {
        private final boolean retryable;

        public AiException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }

    @Value("${docsaas.ai.base-url}")
    private String baseUrl;

    @Value("${docsaas.ai.api-key:}")
    private String apiKey;

    @Value("${docsaas.ai.model}")
    private String model;

    @Value("${docsaas.ai.timeout-seconds:90}")
    private long timeoutSeconds;

    @Value("${docsaas.ai.reasoning-effort:low}")
    private String reasoningEffort;

    @Value("${docsaas.ai.thinking-headroom-tokens:2048}")
    private int thinkingHeadroom;

    private RestClient http;

    @PostConstruct
    void init() {
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        http = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .build();
    }

    public String unavailableReason() {
        boolean local = baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1");
        return (apiKey == null || apiKey.isBlank()) && !local ? "AI features are not set up on this server yet." : null;
    }

    /**
     * Startup check that the key and model work. Uses the model metadata endpoint, so it costs no
     * generation quota. Only logs; never blocks startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkConnection() {
        if (unavailableReason() != null) {
            log.warn("AI is not configured (AI_API_KEY empty): AI tools are shown as unavailable.");
            return;
        }
        Thread.ofVirtual().name("ai-check").start(() -> {
            try {
                http.get().uri("/models/{model}", model).headers(this::auth).retrieve().toBodilessEntity();
                log.info("AI provider OK: model {} at {}", model, baseUrl);
            } catch (RestClientResponseException e) {
                log.error("AI provider check FAILED for model {} (HTTP {}). Check AI_API_KEY and AI_MODEL.", model,
                        e.getStatusCode().value());
            } catch (RuntimeException e) {
                log.warn("AI provider check could not reach {}: {}", baseUrl, e.getMessage());
            }
        });
    }

    /**
     * @param maxTokens the answer's budget. Thinking models count their reasoning against the same
     *                  limit, so {@code thinking-headroom-tokens} is added on top.
     */
    public String complete(List<Message> messages, int maxTokens) {
        try {
            return completeOnce(messages, maxTokens + Math.max(thinkingHeadroom, 0), true);
        } catch (EmptyAnswerException e) {
            // The model spent the whole budget thinking: try once more with a much larger budget.
            log.warn("AI answer was cut off before any text (finish_reason=length); retrying with a larger budget");
            try {
                return completeOnce(messages, (maxTokens + Math.max(thinkingHeadroom, 0)) * 3, true);
            } catch (EmptyAnswerException again) {
                throw new AiException("The AI service returned an empty answer. Please try again.", true);
            }
        }
    }

    /** The model stopped at the token limit without writing any answer text. */
    private static class EmptyAnswerException extends RuntimeException {
    }

    private void auth(org.springframework.http.HttpHeaders h) {
        if (apiKey != null && !apiKey.isBlank()) {
            h.setBearerAuth(apiKey);
        }
    }

    private String completeOnce(List<Message> messages, int maxTokens, boolean withReasoning) {
        List<Map<String, String>> body = new ArrayList<>();
        messages.forEach(m -> body.add(Map.of("role", m.role(), "content", m.content())));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("messages", body);
        request.put("temperature", 0.2);
        request.put("max_tokens", maxTokens);
        boolean reasoning = withReasoning && reasoningEffort != null && !reasoningEffort.isBlank();
        if (reasoning) {
            request.put("reasoning_effort", reasoningEffort.strip());
        }

        JsonNode response;
        try {
            response = http.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(this::auth)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("AI provider returned HTTP {}", status);
            if (status == 400 && reasoning) {
                // Some providers/models don't accept reasoning_effort: retry once without it.
                log.warn("Retrying the AI request without reasoning_effort");
                return completeOnce(messages, maxTokens, false);
            }
            if (status == 429) {
                throw new AiException("The AI service is busy right now. Please try again in a minute.", true);
            }
            if (status >= 500) {
                throw new AiException("The AI service is temporarily unavailable. Please try again.", true);
            }
            if (status == 401 || status == 403) {
                log.error("AI provider rejected the API key - check docsaas.ai.api-key");
            }
            throw new AiException("The AI service could not process this request.", false);
        } catch (ResourceAccessException e) {
            log.warn("AI provider unreachable or timed out");
            throw new AiException("The AI service did not respond in time. Please try again.", true);
        }

        JsonNode choice = response == null ? null : response.path("choices").path(0);
        String content = choice == null ? "" : choice.path("message").path("content").asText("");
        if (content.isBlank() && choice != null && "length".equals(choice.path("finish_reason").asText())) {
            throw new EmptyAnswerException();
        }
        if (content.isBlank()) {
            throw new AiException("The AI service returned an empty answer. Please try again.", true);
        }
        return content.strip();
    }
}
