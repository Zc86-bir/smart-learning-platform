package com.smartlearn.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("wrong_questions")
public class WrongQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long questionId;
    private Long examRecordId;
    private String studentAnswer;
    private String correctAnswer;
    private String difficulty;
    private String category;
    private String knowledgePoint;
    private Integer wrongCount;
    private Boolean mastered;
    private LocalDateTime lastWrongTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getExamRecordId() { return examRecordId; }
    public void setExamRecordId(Long examRecordId) { this.examRecordId = examRecordId; }
    public String getStudentAnswer() { return studentAnswer; }
    public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
    public Boolean getMastered() { return mastered; }
    public void setMastered(Boolean mastered) { this.mastered = mastered; }
    public LocalDateTime getLastWrongTime() { return lastWrongTime; }
    public void setLastWrongTime(LocalDateTime lastWrongTime) { this.lastWrongTime = lastWrongTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
