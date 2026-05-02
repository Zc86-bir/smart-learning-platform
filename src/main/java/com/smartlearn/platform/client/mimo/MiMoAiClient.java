package com.smartlearn.platform.client.mimo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Consumer;

/**
 * MiMo AI async client backed by java.net.http.HttpClient + virtual threads.
 */
@Component
public class MiMoAiClient {

    private static final Logger log = LoggerFactory.getLogger(MiMoAiClient.class);

    private final HttpClient httpClient;
    private final ExecutorService virtualThreadExecutor;
    private final ObjectMapper objectMapper;

    @Value("${mimo.base-url}")
    private String baseUrl;

    @Value("${mimo.api-key}")
    private String apiKey;

    @Value("${mimo.model}")
    private String model;

    @Value("${mimo.connect-timeout-seconds:30}")
    private int connectTimeout;

    @Value("${mimo.request-timeout-seconds:120}")
    private int requestTimeout;

    @Value("${mimo.retry.max-attempts:4}")
    private int maxRetries;

    @Value("${mimo.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${mimo.retry.max-delay-ms:8000}")
    private long maxDelayMs;

    public MiMoAiClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .executor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory()))
            .build();

        this.virtualThreadExecutor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("mimo-", 0).factory()
        );

        this.objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    // ==================== Public API: Chat ====================

    /**
     * Send a chat completion request with virtual-thread async execution.
     */
    public CompletableFuture<String> chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, null, null, null);
    }

    /**
     * Full-parameter chat with thinking and temperature control.
     */
    public CompletableFuture<String> chat(
        String systemPrompt,
        String userPrompt,
        String thinkingType,
        Double temperature,
        Boolean jsonMode
    ) {
        return chat(systemPrompt, userPrompt, thinkingType, temperature, jsonMode, null);
    }

    /**
     * Full-parameter chat with optional model override.
     */
    public CompletableFuture<String> chat(
        String systemPrompt,
        String userPrompt,
        String thinkingType,
        Double temperature,
        Boolean jsonMode,
        String modelOverride
    ) {
        var messages = List.of(
            new MiMoChat.Message("system", systemPrompt),
            new MiMoChat.Message("user", userPrompt)
        );

        var request = buildRequest(messages, thinkingType, temperature, jsonMode, modelOverride);
        return executeWithRetry(request, 1);
    }

    /**
     * Multimodal chat: text + images (for OCR / vision tasks).
     * @param imageParts content parts for each image (base64 data URIs)
     */
    public CompletableFuture<String> chatWithVision(
        String systemPrompt,
        String userText,
        List<MiMoChat.ContentPart> imageParts,
        String thinkingType,
        Double temperature,
        Boolean jsonMode,
        String modelOverride
    ) {
        var messages = List.of(
            new MiMoChat.Message("system", systemPrompt),
            MiMoChat.Message.userWithImages(userText, imageParts)
        );

        var request = buildRequest(messages, thinkingType, temperature, jsonMode, modelOverride);
        return executeWithRetry(request.withMaxTokens(64000), 1);
    }

    /**
     * SSE streaming chat. Each text delta chunk is pushed to the consumer.
     * @param onChunk callback for each streamed text fragment
     * @return CompletableFuture that completes when the stream ends
     */
    public CompletableFuture<Void> streamChat(
        String systemPrompt,
        String userPrompt,
        String thinkingType,
        Double temperature,
        Consumer<String> onChunk
    ) {
        return streamChat(systemPrompt, userPrompt, List.of(), thinkingType, temperature, onChunk);
    }

    /**
     * SSE streaming chat with conversation history.
     */
    public CompletableFuture<Void> streamChat(
        String systemPrompt,
        String userPrompt,
        List<MiMoChat.Message> history,
        String thinkingType,
        Double temperature,
        Consumer<String> onChunk
    ) {
        var messages = new java.util.ArrayList<MiMoChat.Message>();
        messages.add(new MiMoChat.Message("system", systemPrompt));
        messages.addAll(history);
        messages.add(new MiMoChat.Message("user", userPrompt));

        return executeStream(buildRequest(messages, thinkingType, temperature, null), onChunk);
    }

    /**
     * Convenience: generate questions and parse as a List.
     */
    public <T> CompletableFuture<List<T>> chatAndParseList(Class<T> clazz, String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, "disabled", 0.7, true)
            .thenApply(this::stripMarkdown)
            .thenApply(raw -> parseList(raw, clazz));
    }

    /**
     * Convenience: grade a paper and parse the result.
     */
    public <T> CompletableFuture<T> chatAndParse(Class<T> clazz, String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, "enabled", 0.2, true)
            .thenApply(this::stripMarkdown)
            .thenApply(raw -> parse(raw, clazz));
    }

    // ==================== Internal: Chat ====================

    private MiMoChat.Request buildRequest(
        List<MiMoChat.Message> messages,
        String thinkingType,
        Double temperature,
        Boolean jsonMode
    ) {
        return buildRequest(messages, thinkingType, temperature, jsonMode, null);
    }

    private MiMoChat.Request buildRequest(
        List<MiMoChat.Message> messages,
        String thinkingType,
        Double temperature,
        Boolean jsonMode,
        String modelOverride
    ) {
        return new MiMoChat.Request(
            modelOverride != null ? modelOverride : model,
            messages,
            temperature != null ? temperature : 0.7,
            thinkingType != null ? new MiMoChat.Thinking(thinkingType) : null,
            Boolean.TRUE.equals(jsonMode) ? new MiMoChat.ResponseFormat("json_object") : null,
            32000
        );
    }

    private CompletableFuture<String> executeWithRetry(MiMoChat.Request request, int attempt) {
        var body = toJson(request);
        var httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/chat/completions"))
            .header("Content-Type", "application/json")
            .header("api-key", apiKey)
            .timeout(Duration.ofSeconds(requestTimeout))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        log.debug("[MiMo] Attempt {}/{} - model={}", attempt, maxRetries, model);

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .thenCompose(resp -> {
                int status = resp.statusCode();
                if (status == 200) {
                    var response = parseResponse(resp.body());
                    logTokenUsage(response);
                    return CompletableFuture.completedFuture(response.firstContent());
                }
                if (status == 429 && attempt < maxRetries) {
                    var delay = Math.min(initialDelayMs * (1L << (attempt - 1)), maxDelayMs);
                    log.warn("[MiMo] 429 rate limit, retrying in {}ms (attempt {}/{})", delay, attempt + 1, maxRetries);
                    return CompletableFuture.supplyAsync(() -> null,
                        CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS, virtualThreadExecutor))
                        .thenCompose(v -> executeWithRetry(request, attempt + 1));
                }
                log.error("[MiMo] HTTP {} on attempt {}: {}", status, attempt, resp.body());
                return CompletableFuture.failedFuture(
                    new MiMoException("MiMo API error: HTTP " + status + " (attempt " + attempt + ")")
                );
            })
            .orTimeout(requestTimeout, java.util.concurrent.TimeUnit.SECONDS);
    }

    // ==================== Internal: Streaming ====================

    private CompletableFuture<Void> executeStream(MiMoChat.Request request, Consumer<String> onChunk) {
        var body = toJson(request);
        var httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .header("api-key", apiKey)
            .timeout(Duration.ofSeconds(requestTimeout))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        log.debug("[MiMo] Stream request - model={}", model);

        var publisher = new SubmissionPublisher<String>(virtualThreadExecutor, 256);

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.fromLineSubscriber(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription s) {
                this.subscription = s;
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String line) {
                try {
                    if (line.startsWith("data: ")) {
                        var data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            log.info("[MiMo] Stream complete [DONE]");
                            return;
                        }
                        var chunk = MiMoAiClient.this.parseChunk(data);
                        if (chunk != null && chunk.content() != null && !chunk.content().isBlank()) {
                            publisher.submit(chunk.content());
                        }
                    }
                } catch (Exception e) {
                    log.warn("[MiMo] SSE parse error: {}", line, e);
                }
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                log.error("[MiMo] Stream error", t);
                publisher.closeExceptionally(t);
            }

            @Override
            public void onComplete() {
                log.info("[MiMo] Stream ended");
                publisher.close();
            }
        })).exceptionally(ex -> {
            log.error("[MiMo] Stream HTTP error", ex);
            publisher.closeExceptionally(ex);
            return null;
        }).thenApply(v -> null);
    }

    private MiMoChat.StreamChunk parseChunk(String json) {
        try {
            return objectMapper.readValue(json, MiMoChat.StreamChunk.class);
        } catch (JsonProcessingException e) {
            log.debug("[MiMo] SSE chunk parse failed: {}", json);
            return null;
        }
    }

    // ==================== Shared ====================

    public String stripMarkdown(String raw) {
        if (raw == null) return null;
        var cleaned = raw.replaceAll("(?s)```\\w*\\s*\\n?", "");
        cleaned = cleaned.trim();

        // Find the first '[' or '{' that starts a JSON block
        var bracketIdx = -1;
        var braceIdx = -1;
        for (int i = 0; i < cleaned.length(); i++) {
            var c = cleaned.charAt(i);
            if (c == '[' && bracketIdx == -1) { bracketIdx = i; break; }
            if (c == '{' && braceIdx == -1) { braceIdx = i; break; }
        }
        var firstJson = Math.min(
            bracketIdx >= 0 ? bracketIdx : Integer.MAX_VALUE,
            braceIdx >= 0 ? braceIdx : Integer.MAX_VALUE
        );
        if (firstJson > 0 && firstJson < Integer.MAX_VALUE) {
            cleaned = cleaned.substring(firstJson).trim();
        }

        // Also strip trailing text after the JSON by finding the last ']' or '}'
        var lastBracket = -1;
        var lastBrace = -1;
        for (int i = cleaned.length() - 1; i >= 0; i--) {
            var c = cleaned.charAt(i);
            if (c == ']' && lastBracket == -1) { lastBracket = i; break; }
            if (c == '}' && lastBrace == -1) { lastBrace = i; break; }
        }
        var lastJson = Math.max(lastBracket, lastBrace);
        if (lastJson >= 0 && lastJson < cleaned.length() - 1) {
            cleaned = cleaned.substring(0, lastJson + 1).trim();
        }

        // Fix invalid JSON escape sequences: AI often returns LaTeX like \int, \frac
        // with single backslashes that aren't valid JSON escapes. Normalize \X -> \\X.
        cleaned = fixInvalidJsonEscapes(cleaned);

        return cleaned;
    }

    /**
     * Fix JSON escape sequences that the AI sends incorrectly.
     * The AI outputs LaTeX with single backslashes (e.g. \frac, \times, \beta).
     * In JSON, \f=form-feed, \t=tab, \b=backspace — these get decoded wrongly.
     * We escape them to \\f, \\t, \\b so JSON parser preserves the backslash.
     * Also fixes truly invalid escapes like \o (\omega), \p (\pi), etc.
     */
    private String fixInvalidJsonEscapes(String json) {
        // Pass 1: fix truly invalid escapes (\o, \g, \i, etc.)
        json = json.replaceAll("(?<!\\\\)\\\\(?!['\"\\\\/bfnrtu])", "\\\\\\\\");
        // Pass 2: fix valid JSON escapes that are actually LaTeX commands
        // \b in \begin/\beta, \f in \frac/\forall, \n in \neq/\notin, \r in \right, \t in \times/\theta
        // Match when followed by 2+ letters (LaTeX command pattern)
        json = json.replaceAll("(?<!\\\\)\\\\([bfnrt])(?=[a-zA-Z]{2,})", "\\\\\\\\$1");
        return json;
    }

    private MiMoChat.Response parseResponse(String body) {
        try {
            return objectMapper.readValue(body, MiMoChat.Response.class);
        } catch (JsonProcessingException e) {
            throw new MiMoException("Failed to parse MiMo response: " + body, e);
        }
    }

    private <T> T parse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("[MiMo] JSON parse failed: {}", json);
            throw new MiMoException("Failed to parse AI response as " + clazz.getSimpleName(), e);
        }
    }

    private <T> List<T> parseList(String json, Class<T> clazz) {
        try {
            var type = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("[MiMo] JSON list parse failed: {}", json);
            throw new MiMoException("Failed to parse AI response as List<" + clazz.getSimpleName() + ">", e);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new MiMoException("Failed to serialize request", e);
        }
    }

    private void logTokenUsage(MiMoChat.Response response) {
        var usage = response.usage();
        if (usage != null) {
            log.info("[MiMo] Token usage - prompt: {}, completion: {}, total: {}",
                usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
        }
    }

    public void shutdown() {
        virtualThreadExecutor.close();
    }

    /**
     * Fetch available model IDs from the API.
     */
    public CompletableFuture<List<String>> listModels() {
        var httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/models"))
            .header("api-key", apiKey)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .thenApply(resp -> {
                try {
                    var node = objectMapper.readTree(resp.body());
                    var data = node.get("data");
                    var models = new java.util.ArrayList<String>();
                    for (var m : data) {
                        models.add(m.get("id").asText());
                    }
                    return models;
                } catch (JsonProcessingException e) {
                    log.error("[MiMo] Failed to parse models list", e);
                    return List.<String>of();
                }
            });
    }

    // ==================== Exception ====================

    public static class MiMoException extends RuntimeException {
        public MiMoException(String message) { super(message); }
        public MiMoException(String message, Throwable cause) { super(message, cause); }
    }
}
