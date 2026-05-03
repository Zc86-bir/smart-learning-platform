package com.smartlearn.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PracticeStatus {
    IN_PROGRESS("IN_PROGRESS", "练习中"),
    FINISHED("FINISHED", "已完成");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

    PracticeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
