package com.smartlearn.platform.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SmartPaperRequest(
    @NotBlank(message = "试卷标题不能为空")
    String title,

    @NotBlank(message = "分类不能为空")
    String category,

    @NotBlank(message = "学段不能为空")
    String level,

    @NotNull(message = "总分不能为空")
    @Min(value = 1)
    @Max(value = 500)
    int totalScore,

    @NotNull(message = "考试时长不能为空")
    @Min(value = 10)
    @Max(value = 300)
    int durationMinutes,

    // Difficulty distribution: {"EASY": 30, "MEDIUM": 50, "HARD": 20}
    Map<String, Integer> difficultyDist,

    // Type distribution: {"SINGLE_CHOICE": 40, "MULTIPLE_CHOICE": 30, "TRUE_FALSE": 20, "SHORT_ANSWER": 10}
    Map<String, Integer> typeDist,

    String description,

    // Optional: specific knowledge points to focus on
    String knowledgePoint,

    String model
) {}
