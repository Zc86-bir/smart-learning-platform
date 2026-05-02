package com.smartlearn.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.entity.Video;

public interface VideoService {
    Video uploadVideo(Video video);
    Page<Video> listVideos(String category, String status, int page, int size);
    Video approveVideo(Long videoId, Long reviewerId);
    Video rejectVideo(Long videoId, Long reviewerId, String comment);
    String formatDuration(int seconds);
}
