package com.smartlearn.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GradingDetail(
    Long questionId,
    int score,
    int fullScore,
    String correctness,
    String evaluation,
    String suggestion
) {}
