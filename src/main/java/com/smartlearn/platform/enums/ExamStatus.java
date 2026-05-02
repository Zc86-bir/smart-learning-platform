package com.smartlearn.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExamStatus {
    IN_PROGRESS("IN_PROGRESS", "考试中"),
    SUBMITTED("SUBMITTED", "已提交"),
    GRADED("GRADED", "已批阅"),
    FORCE_SUBMITTED("FORCE_SUBMITTED", "强制提交");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

    ExamStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
