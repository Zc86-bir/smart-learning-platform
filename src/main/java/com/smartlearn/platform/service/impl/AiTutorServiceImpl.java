package com.smartlearn.platform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.client.mimo.PromptTemplates;
import com.smartlearn.platform.dto.TutorMessageDTO;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.service.AiChatService;
import com.smartlearn.platform.service.AiTutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AiTutorServiceImpl implements AiTutorService {

    private static final Logger log = LoggerFactory.getLogger(AiTutorServiceImpl.class);
    private static final long MAX_ROUNDS = 10;
    private static final long TTL_MINUTES = 30;

    private final AiChatService aiChatService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AiTutorServiceImpl(
        AiChatService aiChatService,
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper
    ) {
        this.aiChatService = aiChatService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter ask(Long userId, Long questionId, String message, String questionStem, String standardAnswer) {
        var key = "tutor:%s:%s".formatted(userId, questionId);
        var history = loadHistory(key);

        if (history.size() >= MAX_ROUNDS * 2) {
            throw new BizException("对话轮次已达上限（10轮），请重新开始");
        }

        var emitter = new SseEmitter(120_000L);
        var aiResponse = new AtomicReference<StringBuilder>(new StringBuilder());

        aiChatService.streamChat(
            PromptTemplates.TUTOR_SYSTEM,
            PromptTemplates.tutorUserPrompt(questionStem, standardAnswer, message),
            history,
            AiChatService.ChatOptions.builder()
                .temperature(0.7)
                .build(),
            "TUTOR", userId,
            chunk -> {
                aiResponse.get().append(chunk);
                try {
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (IOException e) {
                    log.warn("[Tutor] SSE send failed for user {} question {}", userId, questionId, e);
                    emitter.completeWithError(e);
                }
            }
        ).thenRun(() -> {
            history.add(AiChatService.ChatMessage.user(message));
            history.add(AiChatService.ChatMessage.assistant(aiResponse.get().toString()));
            saveHistory(key, history);
            emitter.complete();
            log.info("[Tutor] Stream completed for user {} question {}", userId, questionId);
        }).exceptionally(ex -> {
            log.error("[Tutor] Stream error for user {} question {}", userId, questionId, ex);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("AI响应出错: " + ex.getMessage()));
            } catch (IOException e) {
                // ignore
            }
            emitter.complete();
            return null;
        });

        return emitter;
    }

    private List<AiChatService.ChatMessage> loadHistory(String key) {
        var json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<AiChatService.ChatMessage>>() {});
        } catch (JsonProcessingException e) {
            log.warn("[Tutor] Failed to load history for key {}: {}", key, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveHistory(String key, List<AiChatService.ChatMessage> history) {
        try {
            var json = objectMapper.writeValueAsString(history);
            stringRedisTemplate.opsForValue().set(key, json, TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("[Tutor] Failed to save history for key {}", key, e);
        }
    }

    @Override
    public void clearHistory(Long userId, Long questionId) {
        var key = "tutor:%s:%s".formatted(userId, questionId);
        stringRedisTemplate.delete(key);
    }

    @Override
    public List<TutorMessageDTO> getHistory(Long userId, Long questionId) {
        var key = "tutor:%s:%s".formatted(userId, questionId);
        var history = loadHistory(key);
        return history.stream()
            .map(m -> new TutorMessageDTO(m.role(), String.valueOf(m.content())))
            .toList();
    }
}
