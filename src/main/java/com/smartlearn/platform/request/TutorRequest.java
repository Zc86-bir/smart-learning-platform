package com.smartlearn.platform.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TutorRequest(
    @NotNull(message = "题目ID不能为空")
    Long questionId,

    @NotBlank(message = "追问内容不能为空")
    String message,

    String questionStem,
    String standardAnswer
) {}
