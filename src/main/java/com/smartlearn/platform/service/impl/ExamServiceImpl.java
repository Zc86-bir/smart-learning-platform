package com.smartlearn.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.client.mimo.PromptTemplates;
import com.smartlearn.platform.dto.GradingDetail;
import com.smartlearn.platform.dto.GradingReport;
import com.smartlearn.platform.dto.LeaderboardEntry;
import com.smartlearn.platform.entity.ExamRecord;
import com.smartlearn.platform.entity.Question;
import com.smartlearn.platform.enums.ExamStatus;
import com.smartlearn.platform.enums.QuestionType;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.ExamRecordMapper;
import com.smartlearn.platform.mapper.PaperMapper;
import com.smartlearn.platform.mapper.QuestionMapper;
import com.smartlearn.platform.service.AiChatService;
import com.smartlearn.platform.service.ExamService;
import com.smartlearn.platform.service.WrongQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
public class ExamServiceImpl implements ExamService {
    private static final String LEADERBOARD_KEY_PREFIX = "leaderboard:";
    private static final String HEARTBEAT_TIMEOUT_KEY = "heartbeat:";
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 120;
    private static final int CUT_SCREEN_FORCE_SUBMIT_THRESHOLD = 5;
    private static final int CUT_SCREEN_WARN_THRESHOLD = 2;
    private static final int CLIPBOARD_WARN_THRESHOLD = 3;
    private static final int FAST_ANSWER_SECONDS = 5;

    private final ExamRecordMapper examRecordMapper;
    private final PaperMapper paperMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionService wrongQuestionService;
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ExecutorService aiExecutor;

    public ExamServiceImpl(
        ExamRecordMapper examRecordMapper,
        PaperMapper paperMapper,
        QuestionMapper questionMapper,
        WrongQuestionService wrongQuestionService,
        AiChatService aiChatService,
        ObjectMapper objectMapper,
        StringRedisTemplate stringRedisTemplate,
        ExecutorService aiExecutor
    ) {
        this.examRecordMapper = examRecordMapper;
        this.paperMapper = paperMapper;
        this.questionMapper = questionMapper;
        this.wrongQuestionService = wrongQuestionService;
        this.aiChatService = aiChatService;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.aiExecutor = aiExecutor;
    }

    @Override
    @Transactional
    public ExamRecord startExam(Long userId, Long paperId) {
        var paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BizException(404, "试卷不存在");

        var record = new ExamRecord();
        record.setUserId(userId);
        record.setPaperId(paperId);
        record.setStatus(ExamStatus.IN_PROGRESS);
        record.setTotalScore(paper.getTotalScore());
        record.setStartTime(LocalDateTime.now());
        record.setLastHeartbeat(LocalDateTime.now());
        record.setCutScreenCount(0);
        record.setClipboardCount(0);
        record.setSuspiciousFlags("[]");
        record.setAnswers("{}");

        examRecordMapper.insert(record);

        // Store heartbeat marker in Redis
        String hbKey = HEARTBEAT_TIMEOUT_KEY + record.getId();
        stringRedisTemplate.opsForValue().set(hbKey, "1", paper.getDurationMinutes() + 10, java.util.concurrent.TimeUnit.MINUTES);

        log.info("[AntiCheat] Exam started: recordId={}, userId={}, paperId={}", record.getId(), userId, paperId);
        return record;
    }

    @Override
    @Transactional
    public void submitAnswer(Long userId, Long examRecordId, String answersJson,
                             int cutScreenCount, int clipboardCount) {
        var record = examRecordMapper.selectById(examRecordId);
        if (record == null) throw new BizException(404, "考试记录不存在");
        if (!record.getUserId().equals(userId)) throw new BizException(403, "无权操作此考试记录");
        if (record.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new BizException("考试已提交，无法重复提交");
        }

        var suspiciousFlags = new ArrayList<String>();
        var now = LocalDateTime.now();
        var duration = (int) Duration.between(record.getStartTime(), now).getSeconds();

        // Check 1: Cut screen analysis
        if (cutScreenCount >= CUT_SCREEN_FORCE_SUBMIT_THRESHOLD) {
            suspiciousFlags.add("EXCESSIVE_CUT_SCREEN:" + cutScreenCount);
            log.warn("[AntiCheat] Excessive cut screen: record={}, count={}", examRecordId, cutScreenCount);
        }

        // Check 2: Clipboard abuse
        if (clipboardCount >= CLIPBOARD_WARN_THRESHOLD) {
            suspiciousFlags.add("CLIPBOARD_ABUSE:" + clipboardCount);
            log.warn("[AntiCheat] Clipboard abuse: record={}, count={}", examRecordId, clipboardCount);
        }

        // Check 3: Fast answer detection
        try {
            var studentAnswers = objectMapper.readValue(answersJson, new TypeReference<Map<String, String>>() {});
            var paper = paperMapper.selectById(record.getPaperId());
            var questionIds = objectMapper.readValue(paper.getQuestionIds(), List.class);
            int fastAnswerCount = 0;
            for (Object qidObj : questionIds) {
                var qid = ((Number) qidObj).longValue();
                var question = questionMapper.selectById(qid);
                if (question != null && question.getType() != QuestionType.SHORT_ANSWER
                    && question.getType() != QuestionType.CODING) {
                    // For objective questions, if answered within FAST_ANSWER_SECONDS, flag it
                    var avgTime = duration / questionIds.size();
                    if (avgTime < FAST_ANSWER_SECONDS && studentAnswers.containsKey(Long.toString(qid))) {
                        fastAnswerCount++;
                    }
                }
            }
            if (fastAnswerCount > questionIds.size() / 2) {
                suspiciousFlags.add("FAST_ANSWER:" + fastAnswerCount + "/" + questionIds.size());
                log.warn("[AntiCheat] Fast answer detected: record={}, {}/{} questions too fast", examRecordId, fastAnswerCount, questionIds.size());
            }
        } catch (Exception e) {
            log.warn("[AntiCheat] Failed to analyze answer speed", e);
        }

        // Determine status
        var status = cutScreenCount >= CUT_SCREEN_FORCE_SUBMIT_THRESHOLD
            ? ExamStatus.FORCE_SUBMITTED : ExamStatus.SUBMITTED;

        record.setStatus(status);
        record.setAnswers(answersJson);
        record.setCutScreenCount(cutScreenCount);
        record.setClipboardCount(clipboardCount);
        record.setSubmitTime(now);
        record.setDurationSeconds(duration);
        if (!suspiciousFlags.isEmpty()) {
            try {
                record.setSuspiciousFlags(objectMapper.writeValueAsString(suspiciousFlags));
            } catch (Exception e) {
                log.warn("[AntiCheat] Failed to serialize flags", e);
            }
        }
        examRecordMapper.updateById(record);

        // Cleanup heartbeat Redis key
        stringRedisTemplate.delete(HEARTBEAT_TIMEOUT_KEY + examRecordId);

        log.info("[AntiCheat] Exam submitted: recordId={}, status={}, suspicious={}, duration={}s",
            examRecordId, status, suspiciousFlags.isEmpty() ? "none" : suspiciousFlags, duration);

        // Async AI grading
        CompletableFuture.runAsync(() -> gradeExam(record), aiExecutor);
    }

    @Override
    public void forceSubmit(Long examRecordId, String reason) {
        var record = examRecordMapper.selectById(examRecordId);
        if (record == null) throw new BizException(404, "考试记录不存在");
        if (record.getStatus() != ExamStatus.IN_PROGRESS) return;

        var flags = parseFlags(record.getSuspiciousFlags());
        flags.add("FORCE_SUBMIT:" + reason);
        record.setStatus(ExamStatus.FORCE_SUBMITTED);
        record.setSubmitTime(LocalDateTime.now());
        try {
            record.setSuspiciousFlags(objectMapper.writeValueAsString(flags));
        } catch (Exception e) {
            log.warn("[AntiCheat] Failed to serialize flags", e);
        }
        examRecordMapper.updateById(record);

        stringRedisTemplate.delete(HEARTBEAT_TIMEOUT_KEY + examRecordId);

        log.info("[AntiCheat] Force submit: recordId={}, reason={}", examRecordId, reason);
    }

    @Override
    public void heartbeat(Long userId, Long examRecordId) {
        var record = examRecordMapper.selectById(examRecordId);
        if (record == null) throw new BizException(404, "考试记录不存在");
        if (!record.getUserId().equals(userId)) throw new BizException(403, "无权操作此考试记录");
        if (record.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new BizException("考试已结束");
        }

        record.setLastHeartbeat(LocalDateTime.now());
        examRecordMapper.updateById(record);

        // Refresh Redis heartbeat TTL
        String hbKey = HEARTBEAT_TIMEOUT_KEY + examRecordId;
        stringRedisTemplate.expire(hbKey, HEARTBEAT_TIMEOUT_SECONDS + 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void reportSuspicious(Long userId, Long examRecordId, String flagType, String detail) {
        var record = examRecordMapper.selectById(examRecordId);
        if (record == null) throw new BizException(404, "考试记录不存在");
        if (!record.getUserId().equals(userId)) throw new BizException(403, "无权操作此考试记录");
        if (record.getStatus() != ExamStatus.IN_PROGRESS) return;

        var flags = parseFlags(record.getSuspiciousFlags());
        var entry = flagType + ":" + detail;
        flags.add(entry);
        try {
            record.setSuspiciousFlags(objectMapper.writeValueAsString(flags));
        } catch (Exception e) {
            log.warn("[AntiCheat] Failed to serialize flags", e);
        }

        // Auto force-submit for critical violations
        if ("MULTI_DEVICE".equals(flagType) || "IP_CHANGE".equals(flagType)) {
            flags.add("CRITICAL_AUTO_SUBMIT");
            record.setStatus(ExamStatus.FORCE_SUBMITTED);
            record.setSubmitTime(LocalDateTime.now());
            log.warn("[AntiCheat] Critical violation auto-submit: recordId={}, flag={}", examRecordId, entry);
        }

        examRecordMapper.updateById(record);
        log.info("[AntiCheat] Suspicious activity reported: recordId={}, flag={}", examRecordId, entry);
    }

    @Override
    public GradingReport getGradingReport(Long examRecordId) {
        var record = examRecordMapper.selectById(examRecordId);
        if (record == null) throw new BizException(404, "考试记录不存在");
        if (record.getStatus() != ExamStatus.GRADED) {
            throw new BizException("考试尚未批阅完成，请稍后查看");
        }
        try {
            return objectMapper.readValue(record.getAiReport(), GradingReport.class);
        } catch (Exception e) {
            throw new BizException("解析AI判卷报告失败");
        }
    }

    @Override
    public List<LeaderboardEntry> getLeaderboard(Long paperId, int limit) {
        String key = LEADERBOARD_KEY_PREFIX + paperId;
        var entries = stringRedisTemplate.opsForZSet()
            .reverseRangeWithScores(key, 0, limit - 1L);

        if (entries == null || entries.isEmpty()) {
            var wrapper = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getPaperId, paperId)
                .in(ExamRecord::getStatus, ExamStatus.GRADED, ExamStatus.SUBMITTED)
                .orderByDesc(ExamRecord::getScore)
                .orderByAsc(ExamRecord::getDurationSeconds)
                .last("LIMIT " + limit);

            var records = examRecordMapper.selectList(wrapper);
            return records.stream()
                .map(this::toLeaderboardEntry)
                .toList();
        }

        return entries.stream()
            .map(typedTuple -> {
                var userId = Long.parseLong(typedTuple.getValue());
                var score = (int) Math.round(typedTuple.getScore());
                return new LeaderboardEntry(userId, "User-" + userId, score, 0, "");
            })
            .toList();
    }

    @Override
    public Map<String, Object> getAntiCheatStatus(Long userId, Long examRecordId) {
        var record = examRecordMapper.selectById(examRecordId);
        if (record == null) throw new BizException(404, "考试记录不存在");
        if (!record.getUserId().equals(userId)) throw new BizException(403, "无权操作");

        var result = new LinkedHashMap<String, Object>();
        result.put("cutScreenCount", record.getCutScreenCount());
        result.put("clipboardCount", record.getClipboardCount());
        result.put("suspiciousFlags", parseFlags(record.getSuspiciousFlags()));
        result.put("status", record.getStatus());

        // Warning level assessment
        int cutCount = record.getCutScreenCount() != null ? record.getCutScreenCount() : 0;
        int clipCount = record.getClipboardCount() != null ? record.getClipboardCount() : 0;
        var flags = parseFlags(record.getSuspiciousFlags());

        String riskLevel;
        if (cutCount >= CUT_SCREEN_FORCE_SUBMIT_THRESHOLD || flags.stream().anyMatch(f -> f.startsWith("CRITICAL"))) {
            riskLevel = "HIGH";
        } else if (cutCount >= CUT_SCREEN_WARN_THRESHOLD || clipCount >= CLIPBOARD_WARN_THRESHOLD) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }
        result.put("riskLevel", riskLevel);

        // Heartbeat check
        String hbKey = HEARTBEAT_TIMEOUT_KEY + examRecordId;
        var hbExists = stringRedisTemplate.hasKey(hbKey);
        if (record.getStatus() == ExamStatus.IN_PROGRESS && !Boolean.TRUE.equals(hbExists)) {
            result.put("heartbeatTimeout", true);
            log.warn("[AntiCheat] Heartbeat timeout: recordId={}", examRecordId);
        }

        return result;
    }

    // ==================== Internal ====================

    void gradeExam(ExamRecord record) {
        try {
            log.info("[ExamService] Starting AI grading for exam {}", record.getId());

            var paper = paperMapper.selectById(record.getPaperId());
            var questionIds = objectMapper.readValue(paper.getQuestionIds(), List.class);
            var studentAnswers = objectMapper.readValue(record.getAnswers(),
                new TypeReference<Map<String, String>>() {});

            var details = new ArrayList<GradingDetail>();
            int totalScore = 0;
            int perScore = paper.getTotalScore() / questionIds.size();

            for (Object qidObj : questionIds) {
                var qid = ((Number) qidObj).longValue();
                var question = questionMapper.selectById(qid);
                if (question == null) continue;

                var studentAns = studentAnswers.getOrDefault(Long.toString(qid), "");
                var detail = gradeSingle(question, studentAns, perScore);
                details.add(detail);
                totalScore += detail.score();

                // Auto-add wrong questions to user's wrong book
                if (!"CORRECT".equals(detail.correctness()) && question.getType() != QuestionType.SHORT_ANSWER) {
                    wrongQuestionService.addWrongQuestion(
                        record.getUserId(), qid,
                        studentAns.isEmpty() ? "未作答" : studentAns,
                        question.getAnswer(), record.getId()
                    );
                }
            }

            var report = new GradingReport(
                totalScore, details,
                "完成度" + (totalScore * 100 / record.getTotalScore()) + "%",
                "建议复习薄弱知识点"
            );

            record.setAiReport(objectMapper.writeValueAsString(report));
            record.setScore(totalScore);
            record.setStatus(ExamStatus.GRADED);
            examRecordMapper.updateById(record);

            String lbKey = LEADERBOARD_KEY_PREFIX + record.getPaperId();
            var sortedScore = totalScore * 10000 - (record.getDurationSeconds() != null ? record.getDurationSeconds() : 0);
            stringRedisTemplate.opsForZSet().add(lbKey, record.getUserId().toString(), sortedScore);

            log.info("[ExamService] AI grading completed for exam {}, score={}/{}", record.getId(), totalScore, record.getTotalScore());
        } catch (Exception e) {
            log.error("[ExamService] AI grading failed for exam {}", record.getId(), e);
        }
    }

    private GradingDetail gradeSingle(Question question, String studentAnswer, int fullScore) {
        try {
            var prompt = PromptTemplates.gradingPrompt(
                question.getType().name(),
                question.getStem(),
                question.getAnswer(),
                studentAnswer.isEmpty() ? "未作答" : studentAnswer,
                fullScore
            );

            var rawJson = aiChatService.chat(
                "你是一位公正的阅卷老师。",
                prompt,
                AiChatService.ChatOptions.builder()
                    .thinkingType("enabled")
                    .temperature(0.2)
                    .build()
            ).get();

            var cleaned = aiChatService.stripMarkdown(rawJson);
            return objectMapper.readValue(cleaned, GradingDetail.class);
        } catch (Exception e) {
            log.warn("[ExamService] AI grading failed for question {}, using fallback", question.getId());
            return fallbackGrade(question, studentAnswer, fullScore);
        }
    }

    private GradingDetail fallbackGrade(Question question, String studentAnswer, int fullScore) {
        var type = question.getType();
        int score;
        String correctness;

        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.TRUE_FALSE) {
            if (question.getAnswer().equalsIgnoreCase(studentAnswer.trim())) {
                score = fullScore;
                correctness = "CORRECT";
            } else {
                score = 0;
                correctness = "WRONG";
            }
        } else if (type == QuestionType.MULTIPLE_CHOICE) {
            var std = question.getAnswer().replaceAll("\\s+", "").toLowerCase();
            var stu = studentAnswer.replaceAll("\\s+", "").toLowerCase();
            if (std.equals(stu)) {
                score = fullScore;
                correctness = "CORRECT";
            } else if (std.contains(stu) || stu.contains(std)) {
                score = fullScore / 2;
                correctness = "PARTIAL";
            } else {
                score = 0;
                correctness = "WRONG";
            }
        } else {
            score = fullScore / 2;
            correctness = "PARTIAL";
        }

        return new GradingDetail(
            question.getId(), score, fullScore, correctness,
            "AI判卷降级为规则匹配", "建议查看标准答案后复习"
        );
    }

    private LeaderboardEntry toLeaderboardEntry(ExamRecord r) {
        var duration = r.getDurationSeconds() != null ? r.getDurationSeconds() : 0;
        var mm = duration / 60;
        var ss = duration % 60;
        return new LeaderboardEntry(
            r.getUserId(), "User-" + r.getUserId(),
            r.getScore() != null ? r.getScore() : 0,
            duration, String.format("%02d:%02d", mm, ss)
        );
    }

    private List<String> parseFlags(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
