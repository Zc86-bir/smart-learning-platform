package com.smartlearn.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartlearn.platform.enums.VideoStatus;

import java.time.LocalDateTime;

@TableName("videos")
public class Video {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private Integer durationSeconds;
    private String durationDisplay;
    private Boolean isOfficial;
    private VideoStatus status;
    private String category;
    private Long uploaderId;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private String reviewComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getDurationDisplay() { return durationDisplay; }
    public void setDurationDisplay(String durationDisplay) { this.durationDisplay = durationDisplay; }
    public Boolean getIsOfficial() { return isOfficial; }
    public void setIsOfficial(Boolean isOfficial) { this.isOfficial = isOfficial; }
    public VideoStatus getStatus() { return status; }
    public void setStatus(VideoStatus status) { this.status = status; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
