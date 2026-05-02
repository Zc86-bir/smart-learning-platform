package com.smartlearn.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaperDTO(
    Long id,
    String title,
    String description,
    Integer totalScore,
    Integer durationMinutes,
    String category,
    LocalDateTime createdAt,
    List<QuestionWithScoreDTO> questions
) {}
