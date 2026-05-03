package com.smartlearn.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.client.mimo.PromptTemplates;
import com.smartlearn.platform.dto.QuestionDTO;
import com.smartlearn.platform.entity.Question;
import com.smartlearn.platform.enums.Difficulty;
import com.smartlearn.platform.enums.QuestionType;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.QuestionMapper;
import com.smartlearn.platform.request.GenerateQuestionsRequest;
import com.smartlearn.platform.service.AiChatService;
import com.smartlearn.platform.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class QuestionServiceImpl implements QuestionService {
    private static final String HOT_QUESTIONS_KEY = "hot:questions";

    private final QuestionMapper questionMapper;
    private final AiChatService aiChatService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public QuestionServiceImpl(
        QuestionMapper questionMapper,
        AiChatService aiChatService,
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper
    ) {
        this.questionMapper = questionMapper;
        this.aiChatService = aiChatService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<QuestionDTO> generateQuestions(GenerateQuestionsRequest request) {
        var systemPrompt = "你是一位资深教育专家，擅长根据知识点出题。";
        var userPrompt = PromptTemplates.generateQuestionsPrompt(
            request.category(),
            request.difficulty(),
            request.count(),
            request.knowledgePoint() != null ? request.knowledgePoint() : "",
            request.types() != null && !request.types().isEmpty()
                ? request.types().toArray(new String[0])
                : new String[0]
        );

        try {
            var rawJson = aiChatService.chat(
                systemPrompt, userPrompt,
                AiChatService.ChatOptions.builder()
                    .thinkingType("disabled")
                    .temperature(0.7)
                    .model(request.model() != null && !request.model().isBlank() ? request.model() : null)
                    .build()
            ).get();
            var cleaned = aiChatService.stripMarkdown(rawJson);

            // Try parsing as array first; if it's an object, extract nested array
            List<QuestionDTO> dtos = new ArrayList<>(parseQuestions(cleaned));

            // Enrich DTOs: AI may miss category/difficulty/type fields
            var targetCategory = request.category();
            var targetDifficulty = Difficulty.valueOf(
                request.difficulty().toUpperCase());

            for (int i = 0; i < dtos.size(); i++) {
                var dto = dtos.get(i);
                var type = dto.type() != null ? dto.type() : QuestionType.SINGLE_CHOICE;
                var category = dto.category() != null && !dto.category().isBlank()
                    ? dto.category() : targetCategory;
                var difficulty = dto.difficulty() != null ? dto.difficulty() : targetDifficulty;

                dtos.set(i, new QuestionDTO(
                    dto.id(), type, category, dto.stem(),
                    dto.options(), dto.answer(), dto.analysis(),
                    difficulty, dto.knowledgePoint()
                ));
            }
            return dtos;
        } catch (Exception e) {
            log.error("[QuestionService] AI question generation failed", e);
            throw new BizException("AI出题失败: " + e.getMessage());
        }
    }

    @Override
    public QuestionDTO saveQuestion(Question question) {
        questionMapper.insert(question);
        return toDTO(question);
    }

    @Override
    public QuestionDTO getQuestionById(Long id) {
        var q = questionMapper.selectById(id);
        if (q == null) throw new BizException(404, "题目不存在");
        incrementHotScore(id);
        return toDTO(q);
    }

    @Override
    public Page<QuestionDTO> listQuestions(String category, String difficulty, String keyword, int page, int size) {
        var wrapper = new LambdaQueryWrapper<Question>();
        if (category != null && !category.isBlank()) {
            wrapper.eq(Question::getCategory, category);
        }
        if (difficulty != null && !difficulty.isBlank()) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Question::getStem, keyword)
                .or().like(Question::getAnswer, keyword);
        }
        wrapper.orderByDesc(Question::getCreatedAt);

        var pageResult = questionMapper.selectPage(new Page<>(page, size), wrapper);
        var dtoPage = new Page<QuestionDTO>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        dtoPage.setRecords(pageResult.getRecords().stream().map(this::toDTO).toList());
        return dtoPage;
    }

    @Override
    public void incrementHotScore(Long questionId) {
        stringRedisTemplate.opsForZSet().incrementScore(HOT_QUESTIONS_KEY, questionId.toString(), 1);
    }

    @Override
    public List<QuestionDTO> getHotQuestions(int limit) {
        var ids = stringRedisTemplate.opsForZSet()
            .reverseRange(HOT_QUESTIONS_KEY, 0, limit - 1L);
        if (ids == null || ids.isEmpty()) return List.of();

        var idList = ids.stream().map(Long::parseLong).toList();
        var questions = questionMapper.selectByIds(idList);
        return questions.stream().map(this::toDTO).toList();
    }

    @Override
    public QuestionDTO updateQuestion(Long id, Question question) {
        var existing = questionMapper.selectById(id);
        if (existing == null) throw new BizException(404, "题目不存在");
        question.setId(id);
        questionMapper.updateById(question);
        return toDTO(questionMapper.selectById(id));
    }

    @Override
    public void deleteQuestion(Long id) {
        questionMapper.deleteById(id);
    }

    @Override
    @Transactional
    public int batchDeleteQuestions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return questionMapper.deleteByIds(ids);
    }

    @Override
    public Map<String, Long> getCategoryStats() {
        return questionMapper.countByCategory().stream()
            .collect(java.util.stream.Collectors.toMap(
                row -> (String) row.get("category"),
                row -> (Long) row.get("cnt")
            ));
    }

    private List<QuestionDTO> parseQuestions(String json) {
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("[QuestionService] JSON parse error, raw length={}", json != null ? json.length() : 0);
            log.error("[QuestionService] raw preview={}", json != null ? json.substring(0, Math.min(500, json.length())) : "null");
            throw new BizException("AI返回格式不正确: " + e.getMessage());
        }
        // Try array
        if (node.isArray()) {
            return cleanDuplicateLatex(objectMapper.convertValue(node, new TypeReference<List<QuestionDTO>>() {}));
        }
        // Try object wrapper with nested array
        if (node.isObject()) {
            for (var key : List.of("questions", "data", "items", "result", "response")) {
                var arr = node.get(key);
                if (arr != null && arr.isArray()) {
                    return cleanDuplicateLatex(objectMapper.convertValue(arr, new TypeReference<List<QuestionDTO>>() {}));
                }
            }
            // Single question object at top level — wrap in list
            if (node.has("stem") && node.has("answer")) {
                try {
                    var single = objectMapper.treeToValue(node, QuestionDTO.class);
                    return cleanDuplicateLatex(List.of(single));
                } catch (Exception e) {
                    throw new BizException("AI返回格式不正确: " + json.substring(0, Math.min(200, json.length())));
                }
            }
        }
        throw new BizException("AI返回格式不正确: " + json.substring(0, Math.min(200, json.length())));
    }

    /**
     * Remove duplicate LaTeX content from question stems.
     * Pattern: $LaTeX$ raw LaTeX -> keeps only $LaTeX$
     */
    private List<QuestionDTO> cleanDuplicateLatex(List<QuestionDTO> dtos) {
        return dtos.stream().map(dto -> new QuestionDTO(
            dto.id(), dto.type(), dto.category(),
            stripDupLatex(dto.stem()),
            cleanOptions(dto.options()),
            stripDupLatex(dto.answer()),
            stripDupLatex(dto.analysis()),
            dto.difficulty(), dto.knowledgePoint()
        )).toList();
    }

    private String stripDupLatex(String text) {
        if (text == null || text.isEmpty()) return text;
        // Remove duplicate LaTeX: "$formula$ rawFormula" or "$formula$rawFormula" -> "$formula$ "
        // The duplicate is the plain-text version of the formula (digits, letters, operators, fractions)
        text = text.replaceAll("\\$([^$]+)\\$\\s*([A-Za-z0-9\\\\(){}_=+\\-*/^,.'\\[\\];:? ]+)", "\\$$1\\$ ");
        // Also catch cases where raw LaTeX follows directly without space
        text = text.replaceAll("\\$([^$]+)\\$([A-Za-z0-9\\\\(){}_=+\\-*/^,.'\\[\\];:? ]+)", "\\$$1\\$ ");
        // Clean up empty $ $ markers left over
        text = text.replaceAll("\\$\\s+\\$", " ");
        text = text.replaceAll("\\$\\$", "");
        return text;
    }

    private Map<String, String> cleanOptions(Map<String, String> options) {
        if (options == null) return null;
        var cleaned = new java.util.HashMap<String, String>();
        options.forEach((k, v) -> {
            if (v != null) {
                v = v.replaceAll("\\$([^$]+)\\$\\s*([A-Za-z0-9\\\\(){}_=+\\-*/^,.'\\[\\];:? ]+)", "\\$$1\\$ ");
                v = v.replaceAll("\\$([^$]+)\\$([A-Za-z0-9\\\\(){}_=+\\-*/^,.'\\[\\];:? ]+)", "\\$$1\\$ ");
                v = v.replaceAll("\\$\\s+\\$", " ");
                v = v.replaceAll("\\$\\$", "");
                cleaned.put(k, v);
            }
        });
        return cleaned;
    }

    private QuestionDTO toDTO(Question q) {
        try {
            var opts = q.getOptions() != null
                ? objectMapper.readValue(q.getOptions(), new TypeReference<Map<String, String>>() {})
                : null;
            return new QuestionDTO(
                q.getId(), q.getType(), q.getCategory(), q.getStem(),
                opts, q.getAnswer(), q.getAnalysis(), q.getDifficulty(), q.getKnowledgePoint()
            );
        } catch (JsonProcessingException e) {
            return new QuestionDTO(
                q.getId(), q.getType(), q.getCategory(), q.getStem(),
                null, q.getAnswer(), q.getAnalysis(), q.getDifficulty(), q.getKnowledgePoint()
            );
        }
    }
}
