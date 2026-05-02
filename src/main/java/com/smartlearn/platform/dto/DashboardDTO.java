package com.smartlearn.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardDTO(
    // Core stats
    Integer totalExams,
    Integer totalQuestions,
    Double avgScore,
    Double avgAccuracy,

    // Score trend: list of {date, score, totalScore, paperTitle}
    List<ScoreTrendItem> scoreTrend,

    // Knowledge point mastery: {knowledgePoint: accuracy%}
    Map<String, Double> knowledgeMastery,

    // Difficulty distribution of wrong questions
    Map<String, Integer> wrongDifficultyDist,

    // Category distribution of wrong questions
    Map<String, Integer> wrongCategoryDist
) {
    public record ScoreTrendItem(
        String date,
        String paperTitle,
        Integer score,
        Integer totalScore
    ) {}
}
