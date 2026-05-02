package com.smartlearn.platform.controller.student;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.dto.WrongQuestionDTO;
import com.smartlearn.platform.service.WrongQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student/wrong-questions")
@Tag(name = "错题本", description = "学生错题管理")
@com.smartlearn.platform.interceptor.RequireRole("STUDENT")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    @GetMapping
    @Operation(summary = "获取错题列表")
    public ApiResponse<Page<WrongQuestionDTO>> list(
        HttpServletRequest request,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String difficulty,
        @RequestParam(required = false) Boolean mastered,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(wrongQuestionService.getWrongQuestions(userId, category, difficulty, mastered, page, size));
    }

    @PostMapping("/{wrongId}/master")
    @Operation(summary = "标记已掌握")
    public ApiResponse<Void> markMastered(HttpServletRequest request, @PathVariable Long wrongId) {
        var userId = (Long) request.getAttribute("userId");
        wrongQuestionService.markMastered(userId, wrongId);
        return ApiResponse.ok();
    }

    @PostMapping("/{wrongId}/reset")
    @Operation(summary = "取消掌握标记")
    public ApiResponse<Void> resetMastered(HttpServletRequest request, @PathVariable Long wrongId) {
        var userId = (Long) request.getAttribute("userId");
        wrongQuestionService.resetMastered(userId, wrongId);
        return ApiResponse.ok();
    }

    @GetMapping("/stats")
    @Operation(summary = "错题分类统计")
    public ApiResponse<Map<String, Integer>> stats(HttpServletRequest request) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(wrongQuestionService.getWrongCategoryStats(userId));
    }
}
