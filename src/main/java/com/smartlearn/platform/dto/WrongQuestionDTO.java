package com.smartlearn.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WrongQuestionDTO(
    Long id,
    Long questionId,
    String stem,
    String type,
    String category,
    String difficulty,
    String knowledgePoint,
    String studentAnswer,
    String correctAnswer,
    String analysis,
    Integer wrongCount,
    Boolean mastered,
    String lastWrongTime
) {}
