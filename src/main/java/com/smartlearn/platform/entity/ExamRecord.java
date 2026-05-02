package com.smartlearn.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartlearn.platform.enums.ExamStatus;

import java.time.LocalDateTime;

@TableName("exam_records")
public class ExamRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long paperId;
    private ExamStatus status;
    private Integer score;
    private Integer totalScore;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer durationSeconds;
    private Integer cutScreenCount;
    private Integer clipboardCount;
    private LocalDateTime lastHeartbeat;
    private String ipAddress;
    private String suspiciousFlags;
    private String answers;
    private String aiReport;

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
    public Long getPaperId() { return paperId; }
    public void setPaperId(Long paperId) { this.paperId = paperId; }
    public ExamStatus getStatus() { return status; }
    public void setStatus(ExamStatus status) { this.status = status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public Integer getCutScreenCount() { return cutScreenCount; }
    public void setCutScreenCount(Integer cutScreenCount) { this.cutScreenCount = cutScreenCount; }
    public Integer getClipboardCount() { return clipboardCount; }
    public void setClipboardCount(Integer clipboardCount) { this.clipboardCount = clipboardCount; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getSuspiciousFlags() { return suspiciousFlags; }
    public void setSuspiciousFlags(String suspiciousFlags) { this.suspiciousFlags = suspiciousFlags; }
    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }
    public String getAiReport() { return aiReport; }
    public void setAiReport(String aiReport) { this.aiReport = aiReport; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
