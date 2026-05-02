package com.smartlearn.platform.controller.student;

import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.dto.QuestionDTO;
import com.smartlearn.platform.entity.Video;
import com.smartlearn.platform.service.BannerService;
import com.smartlearn.platform.service.QuestionService;
import com.smartlearn.platform.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@Tag(name = "学生端", description = "学生公共接口")
@com.smartlearn.platform.interceptor.RequireRole("STUDENT")
public class StudentController {

    private final QuestionService questionService;
    private final VideoService videoService;
    private final BannerService bannerService;

    public StudentController(QuestionService questionService, VideoService videoService, BannerService bannerService) {
        this.questionService = questionService;
        this.videoService = videoService;
        this.bannerService = bannerService;
    }

    @GetMapping("/questions/{id}")
    @Operation(summary = "查看题目详情")
    public ApiResponse<QuestionDTO> getQuestion(@PathVariable Long id) {
        return ApiResponse.ok(questionService.getQuestionById(id));
    }

    @GetMapping("/questions")
    @Operation(summary = "题目列表")
    public ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<QuestionDTO>> listQuestions(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String difficulty,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(questionService.listQuestions(category, difficulty, keyword, page, size));
    }

    @GetMapping("/videos")
    @Operation(summary = "已审核视频列表")
    public ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Video>> listVideos(
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(videoService.listVideos(category, "APPROVED", page, size));
    }

    @GetMapping("/banners")
    @Operation(summary = "活跃轮播图")
    public ApiResponse<List<com.smartlearn.platform.entity.Banner>> listBanners() {
        var page = bannerService.listBanners(1, 50);
        return ApiResponse.ok(page.getRecords().stream()
            .filter(com.smartlearn.platform.entity.Banner::getIsActive)
            .toList());
    }

    @GetMapping("/categories")
    @Operation(summary = "分类统计")
    public ApiResponse<Map<String, Long>> categories() {
        return ApiResponse.ok(questionService.getCategoryStats());
    }
}
