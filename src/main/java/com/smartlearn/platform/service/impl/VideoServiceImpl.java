package com.smartlearn.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlearn.platform.entity.Video;
import com.smartlearn.platform.enums.VideoStatus;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.VideoMapper;
import com.smartlearn.platform.service.VideoService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {

    @Override
    public Video uploadVideo(Video video) {
        video.setStatus(VideoStatus.PENDING);
        video.setDurationDisplay(formatDuration(video.getDurationSeconds()));
        save(video);
        return video;
    }

    @Override
    public Page<Video> listVideos(String category, String status, int page, int size) {
        var wrapper = new LambdaQueryWrapper<Video>();
        if (category != null && !category.isBlank()) {
            wrapper.eq(Video::getCategory, category);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Video::getStatus, status);
        }
        wrapper.orderByDesc(Video::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Video approveVideo(Long videoId, Long reviewerId) {
        var video = getById(videoId);
        if (video == null) throw new BizException(404, "视频不存在");
        video.setStatus(VideoStatus.APPROVED);
        video.setReviewerId(reviewerId);
        video.setReviewedAt(LocalDateTime.now());
        updateById(video);
        return video;
    }

    @Override
    public Video rejectVideo(Long videoId, Long reviewerId, String comment) {
        var video = getById(videoId);
        if (video == null) throw new BizException(404, "视频不存在");
        video.setStatus(VideoStatus.REJECTED);
        video.setReviewerId(reviewerId);
        video.setReviewedAt(LocalDateTime.now());
        video.setReviewComment(comment);
        updateById(video);
        return video;
    }

    @Override
    public String formatDuration(int seconds) {
        if (seconds < 0) throw new BizException("视频时长不能为负数");
        var mm = seconds / 60;
        var ss = seconds % 60;
        return String.format("%02d:%02d", mm, ss);
    }
}
