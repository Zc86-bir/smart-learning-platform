package com.smartlearn.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.client.mimo.PromptTemplates;
import com.smartlearn.platform.dto.PaperDTO;
import com.smartlearn.platform.dto.QuestionDTO;
import com.smartlearn.platform.dto.QuestionWithScoreDTO;
import com.smartlearn.platform.entity.Paper;
import com.smartlearn.platform.entity.Question;
import com.smartlearn.platform.enums.Difficulty;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.PaperMapper;
import com.smartlearn.platform.mapper.QuestionMapper;
import com.smartlearn.platform.request.SmartPaperRequest;
import com.smartlearn.platform.service.AiChatService;
import com.smartlearn.platform.service.PaperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaperServiceImpl implements PaperService {

    private static final Logger log = LoggerFactory.getLogger(PaperServiceImpl.class);

    private final PaperMapper paperMapper;
    private final QuestionMapper questionMapper;
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

    public PaperServiceImpl(PaperMapper paperMapper, QuestionMapper questionMapper,
        AiChatService aiChatService, ObjectMapper objectMapper) {
        this.paperMapper = paperMapper;
        this.questionMapper = questionMapper;
        this.aiChatService = aiChatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Page<Paper> listPapers(int page, int size) {
        return paperMapper.selectPage(new Page<>(page, size), null);
    }

    @Override
    public Paper getPaperById(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BizException("试卷不存在");
        }
        return paper;
    }

    @Override
    public PaperDTO createPaper(Paper paper) {
        paperMapper.insert(paper);
        return getPaperDTO(paper.getId());
    }

    @Override
    public PaperDTO getPaperDTO(Long id) {
        var paper = paperMapper.selectById(id);
        if (paper == null) throw new BizException("试卷不存在");

        List<QuestionWithScoreDTO> questions = new ArrayList<>();
        if (paper.getQuestionIds() != null && !paper.getQuestionIds().isBlank()) {
            try {
                var items = objectMapper.readValue(paper.getQuestionIds(),
                    new TypeReference<List<Map<String, Object>>>() {});
                for (var item : items) {
                    questions.add(objectMapper.convertValue(item, QuestionWithScoreDTO.class));
                }
            } catch (Exception e) {
                log.warn("[PaperService] Failed to parse questionIds: {}", e.getMessage());
            }
        }

        return new PaperDTO(paper.getId(), paper.getTitle(), paper.getDescription(),
            paper.getTotalScore(), paper.getDurationMinutes(),
            extractCategory(paper), paper.getCreatedAt(), questions);
    }

    private String extractCategory(Paper paper) {
        if (paper.getDescription() != null && paper.getDescription().contains("|")) {
            return paper.getDescription().split("\\|")[0];
        }
        return "";
    }

    @Override
    public List<QuestionWithScoreDTO> smartGenerate(SmartPaperRequest request) {
        var typeDist = request.typeDist() != null ? request.typeDist() : defaultTypeDist();

        // Calculate target count per type
        var targetCounts = new LinkedHashMap<String, Integer>();
        for (var typeEntry : typeDist.entrySet()) {
            var type = typeEntry.getKey();
            var percentage = typeEntry.getValue();
            var count = (int) Math.round(request.totalScore() * percentage / 100.0 / 10.0);
            if (count <= 0) count = 1;
            targetCounts.put(type, count);
        }

        // Fetch existing questions from DB, shuffled
        var wrapper = new LambdaQueryWrapper<Question>();
        wrapper.eq(Question::getCategory, request.category());
        var all = questionMapper.selectList(wrapper);
        var shuffled = new ArrayList<>(all);
        java.util.Collections.shuffle(shuffled);

        // Pick existing questions per type, up to target count
        var selected = new ArrayList<QuestionDTO>();
        var typeCounts = new LinkedHashMap<String, Integer>();
        for (var q : shuffled) {
            var type = q.getType().name();
            var current = typeCounts.getOrDefault(type, 0);
            var target = targetCounts.getOrDefault(type, 0);
            if (current < target) {
                selected.add(toDTO(q));
                typeCounts.put(type, current + 1);
            }
        }

        // Generate missing questions via AI
        var needed = new LinkedHashMap<String, Integer>();
        for (var entry : targetCounts.entrySet()) {
            var have = typeCounts.getOrDefault(entry.getKey(), 0);
            if (have < entry.getValue()) {
                needed.put(entry.getKey(), entry.getValue() - have);
            }
        }
        if (!needed.isEmpty()) {
            selected.addAll(generateMissingQuestions(request, needed));
        }

        return buildQuestionWithScores(selected);
    }

    private List<QuestionDTO> generateMissingQuestions(SmartPaperRequest request, Map<String, Integer> needed) {
        int totalNeeded = needed.values().stream().mapToInt(Integer::intValue).sum();
        if (totalNeeded <= 0) return List.of();

        var systemPrompt = "你是一位资深教育专家，擅长根据知识点出题。";
        var userPrompt = PromptTemplates.generateQuestionsPrompt(
            request.category(),
            "MEDIUM",
            totalNeeded,
            request.knowledgePoint() != null ? request.knowledgePoint() : "",
            needed.keySet().toArray(new String[0])
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
            return parseQuestions(cleaned.toString());
        } catch (Exception e) {
            log.error("[PaperService] AI paper generation failed", e);
            throw new BizException("AI智能组卷失败: " + e.getMessage());
        }
    }

    private List<QuestionWithScoreDTO> buildQuestionWithScores(List<QuestionDTO> questions) {
        var result = new ArrayList<QuestionWithScoreDTO>();
        for (var q : questions) {
            int baseScore = 10;
            var diff = q.difficulty() != null ? q.difficulty() : Difficulty.MEDIUM;
            if (diff == Difficulty.HARD) baseScore = 15;
            else if (diff == Difficulty.EASY) baseScore = 5;

            result.add(new QuestionWithScoreDTO(
                q.id(), q.type().name(), q.stem(), q.options(),
                q.answer(), q.analysis(), q.difficulty().name(), baseScore
            ));
        }
        return result;
    }

    private Map<String, Integer> defaultTypeDist() {
        return Map.of("SINGLE_CHOICE", 40, "MULTIPLE_CHOICE", 30, "TRUE_FALSE", 20, "SHORT_ANSWER", 10);
    }

    private List<QuestionDTO> parseQuestions(String json) {
        try {
            var node = objectMapper.readTree(json);
            if (node.isArray()) {
                return objectMapper.convertValue(node, new TypeReference<List<QuestionDTO>>() {});
            }
            if (node.isObject()) {
                for (var key : List.of("questions", "data", "items", "result", "response")) {
                    var arr = node.get(key);
                    if (arr != null && arr.isArray()) {
                        return objectMapper.convertValue(arr, new TypeReference<List<QuestionDTO>>() {});
                    }
                }
                if (node.has("stem") && node.has("answer")) {
                    return List.of(objectMapper.treeToValue(node, QuestionDTO.class));
                }
            }
            throw new BizException("AI返回格式不正确");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("解析AI返回的试卷失败: " + e.getMessage());
        }
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
        } catch (Exception e) {
            return new QuestionDTO(
                q.getId(), q.getType(), q.getCategory(), q.getStem(),
                null, q.getAnswer(), q.getAnalysis(), q.getDifficulty(), q.getKnowledgePoint()
            );
        }
    }
}
