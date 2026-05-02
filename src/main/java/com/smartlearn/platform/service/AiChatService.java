package com.smartlearn.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.client.mimo.MiMoAiClient;
import com.smartlearn.platform.client.mimo.MiMoChat;
import com.smartlearn.platform.entity.AiUsageLog;
import com.smartlearn.platform.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Spring AI-style abstraction over MiMo AI client.
 * Adds: usage logging, Redis caching for idempotent calls, ChatOptions builder.
 * When Spring AI becomes compatible with Spring Boot 4.0, swap implementation.
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final String AI_CACHE_PREFIX = "ai:cache:";
    private static final long AI_CACHE_TTL_MINUTES = 15;

    private final MiMoAiClient miMoClient;
    private final AiUsageService aiUsageService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AiChatService(MiMoAiClient miMoClient, AiUsageService aiUsageService,
                         StringRedisTemplate redisTemplate) {
        this.miMoClient = miMoClient;
        this.aiUsageService = aiUsageService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    // ==================== Chat (ChatClient-style) ====================

    public CompletableFuture<String> chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, null);
    }

    public CompletableFuture<String> chat(String systemPrompt, String userPrompt, ChatOptions options) {
        return chat(systemPrompt, userPrompt, options, null, null);
    }

    public CompletableFuture<String> chat(String systemPrompt, String userPrompt, ChatOptions options,
                                          String purpose, Long userId) {
        return executeWithLogging(() -> miMoClient.chat(
                systemPrompt, userPrompt,
                options != null ? options.thinkingType() : null,
                options != null ? options.temperature() : null,
                options != null ? options.jsonMode() : null,
                options != null ? options.model() : null
            ), purpose, userId, options != null ? options.model() : null,
            () -> cacheKey(systemPrompt, userPrompt, options));
    }

    public CompletableFuture<String> chatWithVision(
        String systemPrompt, String userText, List<MiMoChat.ContentPart> images, ChatOptions options
    ) {
        return chatWithVision(systemPrompt, userText, images, options, null, null);
    }

    public CompletableFuture<String> chatWithVision(
        String systemPrompt, String userText, List<MiMoChat.ContentPart> images,
        ChatOptions options, String purpose, Long userId
    ) {
        return executeWithLogging(() -> miMoClient.chatWithVision(
                systemPrompt, userText, images,
                options != null ? options.thinkingType() : null,
                options != null ? options.temperature() : null,
                options != null ? options.jsonMode() : null,
                options != null ? options.model() : null
            ), purpose, userId, options != null ? options.model() : null, null);
    }

    public CompletableFuture<Void> streamChat(
        String systemPrompt, String userPrompt, List<ChatMessage> history, ChatOptions options,
        Consumer<String> onChunk
    ) {
        return streamChat(systemPrompt, userPrompt, history, options, null, null, onChunk);
    }

    public CompletableFuture<Void> streamChat(
        String systemPrompt, String userPrompt, List<ChatMessage> history, ChatOptions options,
        String purpose, Long userId, Consumer<String> onChunk
    ) {
        var messages = history.stream()
            .map(m -> switch (m.role()) {
                case "system" -> MiMoChat.Message.system(m.content());
                case "assistant" -> MiMoChat.Message.assistant(m.content());
                default -> MiMoChat.Message.user(m.content());
            })
            .toList();

        final long startMs = System.currentTimeMillis();
        return miMoClient.streamChat(
                systemPrompt, userPrompt, messages,
                options != null ? options.thinkingType() : null,
                options != null ? options.temperature() : null, onChunk
            ).whenComplete((v, ex) -> {
                var usageLog = new AiUsageLog();
                usageLog.setUserId(userId);
                usageLog.setModel(options != null ? options.model() : null);
                usageLog.setPurpose(purpose != null ? purpose : "TUTOR");
                usageLog.setDurationMs((int) (System.currentTimeMillis() - startMs));
                if (ex != null) {
                    usageLog.setStatus("FAILED");
                    usageLog.setErrorMessage(ex.getMessage());
                } else {
                    usageLog.setStatus("SUCCESS");
                }
                aiUsageService.log(usageLog);
            });
    }

    // ==================== Structured Output ====================

    public <T> CompletableFuture<T> call(String systemPrompt, String userPrompt, ChatOptions options, Class<T> outputType) {
        return chat(systemPrompt, userPrompt, options).thenApply(this::stripMarkdown)
            .thenApply(raw -> parse(raw, outputType));
    }

    public <T> CompletableFuture<List<T>> callList(String systemPrompt, String userPrompt, ChatOptions options, Class<T> outputType) {
        return chat(systemPrompt, userPrompt, options).thenApply(this::stripMarkdown)
            .thenApply(raw -> parseList(raw, outputType));
    }

    // ==================== Prompt Template ====================

    public String prompt(String template, Map<String, Object> variables) {
        String result = template;
        for (var entry : variables.entrySet()) {
            result = result.replace("%s".formatted(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return result;
    }

    // ==================== Convenience ====================

    public <T> CompletableFuture<List<T>> chatAndParseList(Class<T> clazz, String systemPrompt, String userPrompt) {
        return miMoClient.chatAndParseList(clazz, systemPrompt, userPrompt);
    }

    public <T> CompletableFuture<T> chatAndParse(Class<T> clazz, String systemPrompt, String userPrompt) {
        return miMoClient.chatAndParse(clazz, systemPrompt, userPrompt);
    }

    public CompletableFuture<List<String>> listModels() {
        return miMoClient.listModels();
    }

    public String stripMarkdown(String raw) {
        return miMoClient.stripMarkdown(raw);
    }

    // ==================== Internal: Logging + Caching ====================

    private CompletableFuture<String> executeWithLogging(
        ChatSupplier supplier, String purpose, Long userId, String model, CacheKeySupplier cacheKeySupplier
    ) {
        // Try cache
        if (cacheKeySupplier != null) {
            var cacheKey = cacheKeySupplier.get();
            if (cacheKey != null) {
                var cached = redisTemplate.opsForValue().get(AI_CACHE_PREFIX + cacheKey);
                if (cached != null) {
                    log.info("[AiChat] Cache hit for key={}", cacheKey);
                    return CompletableFuture.completedFuture(cached);
                }
            }
        }

        final long startMs = System.currentTimeMillis();
        return supplier.get()
            .thenApply(result -> {
                var usageLog = new AiUsageLog();
                usageLog.setUserId(userId);
                usageLog.setModel(model);
                usageLog.setPurpose(purpose != null ? purpose : "UNKNOWN");
                usageLog.setDurationMs((int) (System.currentTimeMillis() - startMs));
                usageLog.setStatus("SUCCESS");
                aiUsageService.log(usageLog);

                // Cache result
                if (cacheKeySupplier != null) {
                    var cacheKey = cacheKeySupplier.get();
                    if (cacheKey != null) {
                        redisTemplate.opsForValue().set(AI_CACHE_PREFIX + cacheKey, result,
                            AI_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                    }
                }
                return result;
            })
            .exceptionally(ex -> {
                var usageLog = new AiUsageLog();
                usageLog.setUserId(userId);
                usageLog.setModel(model);
                usageLog.setPurpose(purpose != null ? purpose : "UNKNOWN");
                usageLog.setDurationMs((int) (System.currentTimeMillis() - startMs));
                usageLog.setStatus("FAILED");
                usageLog.setErrorMessage(ex.getMessage());
                aiUsageService.log(usageLog);
                throw new BizException("AI调用失败: " + ex.getMessage());
            });
    }

    private String cacheKey(String systemPrompt, String userPrompt, ChatOptions options) {
        try {
            var raw = systemPrompt + "|" + userPrompt + "|"
                + (options != null ? options.model() : "") + "|"
                + (options != null ? options.temperature() : "") + "|"
                + (options != null ? options.thinkingType() : "");
            var md = MessageDigest.getInstance("MD5");
            var digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private <T> T parse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("[AiChat] JSON parse failed: {}", json);
            throw new BizException("AI响应解析失败: " + e.getMessage());
        }
    }

    private <T> List<T> parseList(String json, Class<T> clazz) {
        try {
            var type = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("[AiChat] JSON list parse failed: {}", json);
            throw new BizException("AI响应解析失败: " + e.getMessage());
        }
    }

    // ==================== Functional interfaces ====================

    @FunctionalInterface
    interface ChatSupplier {
        CompletableFuture<String> get();
    }

    @FunctionalInterface
    interface CacheKeySupplier {
        String get();
    }

    // ==================== Records ====================

    public record ChatOptions(
        String model,
        String thinkingType,
        Double temperature,
        Boolean jsonMode
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String model;
            private String thinkingType;
            private Double temperature;
            private Boolean jsonMode;

            public Builder model(String model) { this.model = model; return this; }
            public Builder thinkingType(String thinkingType) { this.thinkingType = thinkingType; return this; }
            public Builder temperature(Double temperature) { this.temperature = temperature; return this; }
            public Builder jsonMode(Boolean jsonMode) { this.jsonMode = jsonMode; return this; }

            public ChatOptions build() {
                return new ChatOptions(model, thinkingType, temperature, jsonMode);
            }
        }
    }

    public record ChatMessage(String role, String content) {
        public static ChatMessage user(String text) { return new ChatMessage("user", text); }
        public static ChatMessage assistant(String text) { return new ChatMessage("assistant", text); }
        public static ChatMessage system(String text) { return new ChatMessage("system", text); }
    }

    public record ImagePart(String mimeType, byte[] data) {
        public MiMoChat.ContentPart toContentPart() {
            var base64 = java.util.Base64.getEncoder().encodeToString(data);
            return MiMoChat.ContentPart.imageUrl("data:" + mimeType + ";base64," + base64, "high");
        }
    }
}
