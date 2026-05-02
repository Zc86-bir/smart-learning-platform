package com.smartlearn.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.enums.Difficulty;
import com.smartlearn.platform.enums.QuestionType;

import java.time.LocalDateTime;

@TableName("questions")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private QuestionType type;
    private String category;
    private String stem;
    private String options;
    private String answer;
    private String analysis;
    private Difficulty difficulty;
    private String knowledgePoint;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public QuestionType getType() { return type; }
    public void setType(QuestionType type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }
    public String getOptions() { return options; }

    @com.fasterxml.jackson.annotation.JsonSetter
    public void setOptions(Object options) {
        if (options == null) {
            this.options = null;
        } else if (options instanceof String s) {
            this.options = s;
        } else {
            try {
                this.options = new ObjectMapper().writeValueAsString(options);
            } catch (JsonProcessingException e) {
                this.options = options.toString();
            }
        }
    }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
