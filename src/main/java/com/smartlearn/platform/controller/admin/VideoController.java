package com.smartlearn.platform.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.annotation.LogOperation;
import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.entity.Video;
import com.smartlearn.platform.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/videos")
@Tag(name = "视频管理(管理员)", description = "视频上传、审核")
@com.smartlearn.platform.interceptor.RequireRole("ADMIN")
public class VideoController {

    private static final Path UPLOAD_DIR = Paths.get("uploads/videos");

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
        try { Files.createDirectories(UPLOAD_DIR); } catch (IOException ignored) {}
    }

    @PostMapping
    @Operation(summary = "上传视频")
    @LogOperation(module = "video", operation = "UPLOAD")
    public ApiResponse<Video> upload(
        @RequestParam("title") String title,
        @RequestParam("durationSeconds") int durationSeconds,
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Boolean isOfficial
    ) throws IOException {
        var ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
        var filename = UUID.randomUUID() + ext;
        var target = UPLOAD_DIR.resolve(filename);
        file.transferTo(target);

        var video = new Video();
        video.setTitle(title);
        video.setDurationSeconds(durationSeconds);
        video.setDurationDisplay(videoService.formatDuration(durationSeconds));
        video.setVideoUrl("/uploads/videos/" + filename);
        video.setCategory(category);
        video.setIsOfficial(isOfficial != null && isOfficial);

        return ApiResponse.ok(videoService.uploadVideo(video));
    }

    @GetMapping
    @Operation(summary = "视频列表")
    public ApiResponse<Page<Video>> list(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(videoService.listVideos(category, status, page, size));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审核通过")
    @LogOperation(module = "video", operation = "APPROVE")
    public ApiResponse<Video> approve(HttpServletRequest request, @PathVariable Long id) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(videoService.approveVideo(id, userId));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审核拒绝")
    @LogOperation(module = "video", operation = "REJECT")
    public ApiResponse<Video> reject(
        HttpServletRequest request,
        @PathVariable Long id,
        @RequestParam String comment
    ) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(videoService.rejectVideo(id, userId, comment));
    }
}
