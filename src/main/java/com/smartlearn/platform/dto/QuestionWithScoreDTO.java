package com.smartlearn.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionWithScoreDTO(
    Long questionId,
    String type,
    String stem,
    Map<String, String> options,
    String answer,
    String analysis,
    String difficulty,
    int score
) {}
