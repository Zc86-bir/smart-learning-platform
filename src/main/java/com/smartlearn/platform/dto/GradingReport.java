package com.smartlearn.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GradingReport(
    int totalScore,
    List<GradingDetail> details,
    String overallEvaluation,
    String suggestion
) {}
