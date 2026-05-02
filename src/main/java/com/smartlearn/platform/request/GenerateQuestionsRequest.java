package com.smartlearn.platform.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record GenerateQuestionsRequest(
    @NotBlank(message = "分类不能为空")
    String category,

    @NotNull(message = "难度不能为空")
    String difficulty,

    @Positive(message = "题目数量必须大于0")
    @Max(value = 50, message = "单次最多生成50题")
    int count,

    String knowledgePoint,

    String model,

    List<String> types
) {}
