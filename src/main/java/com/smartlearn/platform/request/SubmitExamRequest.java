package com.smartlearn.platform.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitExamRequest(
    @NotNull(message = "考试记录ID不能为空")
    Long examRecordId,

    @NotBlank(message = "作答内容不能为空")
    String answers,

    Integer cutScreenCount,
    Integer clipboardCount
) {}
