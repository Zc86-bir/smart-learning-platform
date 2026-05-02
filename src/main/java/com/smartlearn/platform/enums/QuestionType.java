package com.smartlearn.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QuestionType {

    SINGLE_CHOICE("SINGLE_CHOICE", "单选题"),
    MULTIPLE_CHOICE("MULTIPLE_CHOICE", "多选题"),
    TRUE_FALSE("TRUE_FALSE", "判断题"),
    SHORT_ANSWER("SHORT_ANSWER", "简答题"),
    CODING("CODING", "编程题");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

    QuestionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
