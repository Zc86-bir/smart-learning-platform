package com.smartlearn.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.dto.WrongQuestionDTO;
import com.smartlearn.platform.entity.Question;
import com.smartlearn.platform.entity.WrongQuestion;
import com.smartlearn.platform.mapper.QuestionMapper;
import com.smartlearn.platform.mapper.WrongQuestionMapper;
import com.smartlearn.platform.service.WrongQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class WrongQuestionServiceImpl implements WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;

    public WrongQuestionServiceImpl(WrongQuestionMapper wrongQuestionMapper, QuestionMapper questionMapper) {
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.questionMapper = questionMapper;
    }

    @Override
    @Transactional
    public void addWrongQuestion(Long userId, Long questionId, String studentAnswer,
                                  String correctAnswer, Long examRecordId) {
        var wrapper = new LambdaQueryWrapper<WrongQuestion>()
            .eq(WrongQuestion::getUserId, userId)
            .eq(WrongQuestion::getQuestionId, questionId);
        var existing = wrongQuestionMapper.selectOne(wrapper);

        if (existing != null) {
            // Already in wrong book, increment count
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setLastWrongTime(LocalDateTime.now());
            existing.setMastered(false);
            wrongQuestionMapper.updateById(existing);
        } else {
            var question = questionMapper.selectById(questionId);
            if (question == null) return;

            var wrong = new WrongQuestion();
            wrong.setUserId(userId);
            wrong.setQuestionId(questionId);
            wrong.setExamRecordId(examRecordId);
            wrong.setStudentAnswer(studentAnswer);
            wrong.setCorrectAnswer(correctAnswer);
            wrong.setDifficulty(question.getDifficulty().name());
            wrong.setCategory(question.getCategory());
            wrong.setKnowledgePoint(question.getKnowledgePoint());
            wrong.setWrongCount(1);
            wrong.setMastered(false);
            wrong.setLastWrongTime(LocalDateTime.now());
            wrongQuestionMapper.insert(wrong);
        }
    }

    @Override
    public Page<WrongQuestionDTO> getWrongQuestions(Long userId, String category,
                                                     String difficulty, Boolean mastered,
                                                     int page, int size) {
        var p = new Page<WrongQuestion>(page, size);
        var wrapper = new LambdaQueryWrapper<WrongQuestion>()
            .eq(WrongQuestion::getUserId, userId);
        if (category != null && !category.isBlank()) wrapper.eq(WrongQuestion::getCategory, category);
        if (difficulty != null && !difficulty.isBlank()) wrapper.eq(WrongQuestion::getDifficulty, difficulty);
        if (mastered != null) wrapper.eq(WrongQuestion::getMastered, mastered);
        wrapper.orderByDesc(WrongQuestion::getWrongCount);
        var wrongPage = wrongQuestionMapper.selectPage(p, wrapper);

        // Batch load questions to avoid N+1
        var questionIds = wrongPage.getRecords().stream()
            .map(WrongQuestion::getQuestionId)
            .toList();
        var questionMap = questionIds.isEmpty()
            ? Map.<Long, Question>of()
            : questionMapper.selectByIds(questionIds).stream()
                .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q));

        // Enrich with question details
        var dtoRecords = wrongPage.getRecords().stream()
            .map(w -> {
                var q = questionMap.get(w.getQuestionId());
                String lastTime = w.getLastWrongTime() != null
                    ? w.getLastWrongTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : null;
                return new WrongQuestionDTO(
                    w.getId(), w.getQuestionId(),
                    q != null ? q.getStem() : null,
                    q != null ? q.getType().name() : null,
                    q != null ? q.getCategory() : w.getCategory(),
                    w.getDifficulty(),
                    q != null ? q.getKnowledgePoint() : w.getKnowledgePoint(),
                    w.getStudentAnswer(), w.getCorrectAnswer(),
                    q != null ? q.getAnalysis() : null,
                    w.getWrongCount(), w.getMastered(), lastTime
                );
            })
            .toList();

        var result = new Page<WrongQuestionDTO>(page, size, wrongPage.getTotal());
        result.setRecords(dtoRecords);
        return result;
    }

    @Override
    public void markMastered(Long userId, Long wrongId) {
        var rows = wrongQuestionMapper.update(null,
            new LambdaUpdateWrapper<WrongQuestion>()
                .eq(WrongQuestion::getId, wrongId)
                .eq(WrongQuestion::getUserId, userId)
                .set(WrongQuestion::getMastered, true));
        if (rows > 0) {
            log.info("[WrongBook] User {} marked question {} as mastered", userId, wrongId);
        }
    }

    @Override
    public void resetMastered(Long userId, Long wrongId) {
        wrongQuestionMapper.update(null,
            new LambdaUpdateWrapper<WrongQuestion>()
                .eq(WrongQuestion::getId, wrongId)
                .eq(WrongQuestion::getUserId, userId)
                .set(WrongQuestion::getMastered, false));
    }

    @Override
    public Map<String, Integer> getWrongCategoryStats(Long userId) {
        var wrapper = new LambdaQueryWrapper<WrongQuestion>()
            .eq(WrongQuestion::getUserId, userId)
            .eq(WrongQuestion::getMastered, false);
        var all = wrongQuestionMapper.selectList(wrapper);

        var stats = new LinkedHashMap<String, Integer>();
        for (var w : all) {
            var cat = w.getCategory() != null ? w.getCategory() : "未知";
            stats.merge(cat, 1, (a, b) -> a + b);
        }
        return stats;
    }
}
