package com.smartlearn.platform.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PracticeStartRequest(
    String category,
    String difficulty,
    @Min(1) @Max(50) int count
) {}
