package com.smartlearn.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.dto.QuestionDTO;
import com.smartlearn.platform.entity.PracticeSession;
import com.smartlearn.platform.entity.Question;
import com.smartlearn.platform.enums.PracticeStatus;
import com.smartlearn.platform.mapper.PracticeSessionMapper;
import com.smartlearn.platform.mapper.QuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PracticeServiceImpl implements PracticeService {

    private final QuestionMapper questionMapper;
    private final PracticeSessionMapper practiceSessionMapper;
    private final ObjectMapper objectMapper;

    public PracticeServiceImpl(QuestionMapper questionMapper, PracticeSessionMapper practiceSessionMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.practiceSessionMapper = practiceSessionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PracticeResult startPractice(Long userId, String category, String difficulty, int count) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getDeleted, 0);
        if (category != null && !category.isBlank()) wrapper.eq(Question::getCategory, category);
        if (difficulty != null && !difficulty.isBlank()) wrapper.eq(Question::getDifficulty, difficulty);
        wrapper.select(Question::getId);

        List<Question> allQuestions = questionMapper.selectList(wrapper);
        if (allQuestions.isEmpty()) {
            throw new IllegalArgumentException("没有找到符合条件的题目");
        }

        int actualCount = Math.min(count, allQuestions.size());
        Collections.shuffle(allQuestions);
        List<Long> selectedIds = allQuestions.subList(0, actualCount).stream()
            .map(Question::getId)
            .toList();

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setCategory(category);
        session.setDifficulty(difficulty);
        session.setQuestionCount(actualCount);
        try {
            session.setQuestionIds(objectMapper.writeValueAsString(selectedIds));
        } catch (Exception e) {
            throw new RuntimeException("序列化题目ID失败", e);
        }
        session.setAnswers("[]");
        session.setCorrectCount(0);
        session.setStatus(PracticeStatus.IN_PROGRESS);
        session.setStartTime(LocalDateTime.now());
        practiceSessionMapper.insert(session);

        List<QuestionDTO> questions = selectedIds.stream()
            .map(id -> questionMapper.selectById(id))
            .filter(q -> q != null && q.getDeleted() == 0)
            .map(this::toDTO)
            .collect(Collectors.toList());

        return new PracticeResult(session.getId(), questions);
    }

    @Override
    @Transactional
    public PracticeAnswerResult submitAnswer(Long userId, Long sessionId, Long questionId, String answer) {
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("练习会话不存在");
        }
        if (session.getStatus() != PracticeStatus.IN_PROGRESS) {
            throw new IllegalStateException("练习已结束");
        }

        Question question = questionMapper.selectById(questionId);
        if (question == null || question.getDeleted() != 0) {
            throw new IllegalArgumentException("题目不存在");
        }

        boolean correct = checkAnswer(question, answer);
        if (correct) {
            session.setCorrectCount(session.getCorrectCount() + 1);
        }

        try {
            List<Map<String, Object>> answers = objectMapper.readValue(
                session.getAnswers(), new TypeReference<List<Map<String, Object>>>() {});
            Map<String, Object> answerEntry = new LinkedHashMap<>();
            answerEntry.put("questionId", questionId);
            answerEntry.put("userAnswer", answer != null ? answer : "");
            answerEntry.put("correct", correct);
            answers.add(answerEntry);
            session.setAnswers(objectMapper.writeValueAsString(answers));
        } catch (Exception e) {
            throw new RuntimeException("保存答案失败", e);
        }

        practiceSessionMapper.updateById(session);

        return new PracticeAnswerResult(questionId, correct, question.getAnswer(), question.getAnalysis());
    }

    @Override
    @Transactional
    public PracticeReport finishPractice(Long userId, Long sessionId) {
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("练习会话不存在");
        }
        session.setStatus(PracticeStatus.FINISHED);
        session.setFinishTime(LocalDateTime.now());
        int accuracy = session.getQuestionCount() > 0
            ? (int) Math.round(100.0 * session.getCorrectCount() / session.getQuestionCount())
            : 0;
        session.setAccuracy(accuracy);
        practiceSessionMapper.updateById(session);

        return buildReport(session);
    }

    @Override
    public PracticeReport getSessionReport(Long userId, Long sessionId) {
        PracticeSession session = practiceSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("练习会话不存在");
        }
        return buildReport(session);
    }

    private boolean checkAnswer(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) return false;
        String correct = question.getAnswer();
        if (correct == null) return false;

        return switch (question.getType()) {
            case SINGLE_CHOICE, TRUE_FALSE ->
                correct.trim().equalsIgnoreCase(userAnswer.trim());
            case MULTIPLE_CHOICE -> {
                Set<String> correctSet = normalizeOptions(correct);
                Set<String> userSet = normalizeOptions(userAnswer);
                yield correctSet.equals(userSet);
            }
            case SHORT_ANSWER, CODING -> {
                String normalizedCorrect = correct.toLowerCase().replaceAll("\\s+", "");
                String normalizedUser = userAnswer.toLowerCase().replaceAll("\\s+", "");
                yield normalizedUser.contains(normalizedCorrect.substring(0, Math.min(20, normalizedCorrect.length())))
                    || normalizedCorrect.contains(normalizedUser.substring(0, Math.min(20, normalizedUser.length())));
            }
            default -> false;
        };
    }

    private Set<String> normalizeOptions(String input) {
        return Arrays.stream(input.split("[,，、]"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    private QuestionDTO toDTO(Question q) {
        try {
            var opts = q.getOptions() != null
                ? objectMapper.readValue(q.getOptions(), new TypeReference<Map<String, String>>() {})
                : null;
            // Practice mode: exclude answer from initial display
            return new QuestionDTO(
                q.getId(), q.getType(), q.getCategory(), q.getStem(),
                opts, null, null, q.getDifficulty(), q.getKnowledgePoint()
            );
        } catch (Exception e) {
            return new QuestionDTO(
                q.getId(), q.getType(), q.getCategory(), q.getStem(),
                null, null, null, q.getDifficulty(), q.getKnowledgePoint()
            );
        }
    }

    private PracticeReport buildReport(PracticeSession session) {
        List<Long> questionIds;
        try {
            questionIds = objectMapper.readValue(session.getQuestionIds(), new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            throw new RuntimeException("解析题目ID失败", e);
        }

        List<Map<String, Object>> answerList;
        try {
            answerList = objectMapper.readValue(session.getAnswers(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            answerList = Collections.emptyList();
        }

        Map<Long, Map<String, Object>> answerMap = answerList.stream()
            .collect(Collectors.toMap(
                m -> ((Number) m.get("questionId")).longValue(),
                m -> m
            ));

        List<PracticeReport.QuestionDetail> questionDetails = questionIds.stream()
            .map(id -> questionMapper.selectById(id))
            .filter(q -> q != null && q.getDeleted() == 0)
            .map(q -> {
                Map<String, Object> ans = answerMap.get(q.getId());
                String userAns = ans != null ? (String) ans.get("userAnswer") : "";
                boolean isCorrect = ans != null && Boolean.TRUE.equals(ans.get("correct"));
                return new PracticeReport.QuestionDetail(
                    q.getId(), q.getStem(), q.getType().name(),
                    q.getDifficulty().name(),
                    userAns != null ? userAns : "", q.getAnswer(),
                    q.getAnalysis(), isCorrect
                );
            })
            .collect(Collectors.toList());

        int accuracy = session.getQuestionCount() != null && session.getQuestionCount() > 0
            ? (int) Math.round(100.0 * session.getCorrectCount() / session.getQuestionCount())
            : 0;

        return new PracticeReport(
            session.getId(), session.getCategory(),
            session.getQuestionCount(), session.getCorrectCount(),
            accuracy, questionDetails
        );
    }
}
