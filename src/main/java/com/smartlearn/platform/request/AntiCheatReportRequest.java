package com.smartlearn.platform.request;

import jakarta.validation.constraints.NotNull;

public record AntiCheatReportRequest(
    @NotNull(message = "考试记录ID不能为空")
    Long examRecordId,

    Integer cutScreenCount,
    Integer clipboardCount,
    String clientIp
) {}
