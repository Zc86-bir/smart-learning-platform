package com.smartlearn.platform.request;

import jakarta.validation.constraints.NotNull;

public record PracticeAnswerRequest(
    @NotNull Long questionId,
    String answer
) {}
