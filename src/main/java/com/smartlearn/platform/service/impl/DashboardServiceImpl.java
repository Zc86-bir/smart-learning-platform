package com.smartlearn.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.dto.DashboardDTO;
import com.smartlearn.platform.dto.GradingReport;
import com.smartlearn.platform.entity.ExamRecord;
import com.smartlearn.platform.entity.WrongQuestion;
import com.smartlearn.platform.enums.ExamStatus;
import com.smartlearn.platform.mapper.ExamRecordMapper;
import com.smartlearn.platform.mapper.WrongQuestionMapper;
import com.smartlearn.platform.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ExamRecordMapper examRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ObjectMapper objectMapper;

    public DashboardServiceImpl(ExamRecordMapper examRecordMapper,
        WrongQuestionMapper wrongQuestionMapper,
        ObjectMapper objectMapper) {
        this.examRecordMapper = examRecordMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public DashboardDTO getDashboard(Long userId) {
        // 1. Exam stats
        var gradedWrapper = new LambdaQueryWrapper<ExamRecord>()
            .eq(ExamRecord::getUserId, userId)
            .in(ExamRecord::getStatus, ExamStatus.GRADED, ExamStatus.SUBMITTED)
            .orderByDesc(ExamRecord::getCreatedAt);
        var allRecords = examRecordMapper.selectList(gradedWrapper);

        int totalExams = allRecords.size();
        double avgScore = allRecords.stream()
            .filter(r -> r.getScore() != null)
            .mapToInt(ExamRecord::getScore)
            .average().orElse(0.0);

        double avgAccuracy = allRecords.stream()
            .filter(r -> r.getScore() != null && r.getTotalScore() != null && r.getTotalScore() > 0)
            .mapToDouble(r -> (double) r.getScore() / r.getTotalScore())
            .average().orElse(0.0);

        // 2. Total wrong question count (not mastered)
        int totalWrong = wrongQuestionMapper.selectCount(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getMastered, false)
        ).intValue();

        // 3. Score trend (last 10 exams)
        var scoreTrend = allRecords.stream()
            .limit(10)
            .map(r -> new DashboardDTO.ScoreTrendItem(
                r.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd")),
                "考试#" + r.getPaperId(),
                r.getScore(),
                r.getTotalScore()
            ))
            .toList();

        // 4. Knowledge point mastery from grading reports
        var knowledgeAccuracy = new LinkedHashMap<String, Integer[]>(); // [correct, total]

        for (var record : allRecords) {
            if (record.getAiReport() == null) continue;
            try {
                var report = objectMapper.readValue(record.getAiReport(), GradingReport.class);
                if (report.details() != null) {
                    for (var detail : report.details()) {
                        boolean correct = "CORRECT".equals(detail.correctness());

                        var kp = detail.questionId() != null ? "Q" + detail.questionId() : "未知";
                        knowledgeAccuracy.computeIfAbsent(kp, k -> new Integer[]{0, 0});
                        knowledgeAccuracy.get(kp)[1]++;
                        if (correct) knowledgeAccuracy.get(kp)[0]++;
                    }
                }
            } catch (Exception e) {
                // skip bad report
            }
        }

        var knowledgeMastery = new LinkedHashMap<String, Double>();
        for (var entry : knowledgeAccuracy.entrySet()) {
            var v = entry.getValue();
            if (v[1] > 0) {
                knowledgeMastery.put(entry.getKey(), (double) Math.round((double) v[0] / v[1] * 100.0));
            }
        }

        // 5. Wrong question difficulty distribution
        var wrongDiffDist = new LinkedHashMap<String, Integer>();
        var wrongCategoryDist = new LinkedHashMap<String, Integer>();
        var wrongQuestions = wrongQuestionMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getMastered, false)
        );
        for (var w : wrongQuestions) {
            wrongDiffDist.merge(w.getDifficulty() != null ? w.getDifficulty() : "MEDIUM", 1, (a, b) -> a + b);
            String cat = w.getCategory() != null ? w.getCategory() : "未知";
            wrongCategoryDist.merge(cat, 1, (a, b) -> a + b);
        }

        return new DashboardDTO(
            totalExams, totalWrong,
            Math.round(avgScore * 100.0) / 100.0,
            Math.round(avgAccuracy * 100.0 * 100.0) / 100.0,
            scoreTrend, knowledgeMastery,
            wrongDiffDist, wrongCategoryDist
        );
    }
}
