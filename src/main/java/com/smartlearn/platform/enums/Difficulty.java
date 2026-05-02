package com.smartlearn.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Difficulty {
    EASY("EASY", "简单"),
    MEDIUM("MEDIUM", "中等"),
    HARD("HARD", "困难");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

    Difficulty(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
