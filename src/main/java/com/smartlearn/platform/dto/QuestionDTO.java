package com.smartlearn.platform.dto;

import com.smartlearn.platform.enums.Difficulty;
import com.smartlearn.platform.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionDTO(
    Long id,
    QuestionType type,
    String category,
    String stem,
    Map<String, String> options,
    String answer,
    String analysis,
    Difficulty difficulty,
    String knowledgePoint
) {}
