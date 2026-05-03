package com.smartlearn.platform.controller.student;

import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.request.PracticeAnswerRequest;
import com.smartlearn.platform.request.PracticeStartRequest;
import com.smartlearn.platform.service.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/practice")
@Tag(name = "学生端-练习", description = "题库练习接口")
@com.smartlearn.platform.interceptor.RequireRole("STUDENT")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/start")
    @Operation(summary = "开始练习")
    public ApiResponse<PracticeService.PracticeResult> startPractice(
        HttpServletRequest request,
        @Valid @RequestBody PracticeStartRequest req
    ) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(practiceService.startPractice(userId, req.category(), req.difficulty(), req.count()));
    }

    @PostMapping("/{sessionId}/answer")
    @Operation(summary = "提交单题答案")
    public ApiResponse<PracticeService.PracticeAnswerResult> submitAnswer(
        HttpServletRequest request,
        @PathVariable Long sessionId,
        @Valid @RequestBody PracticeAnswerRequest req
    ) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(practiceService.submitAnswer(userId, sessionId, req.questionId(), req.answer()));
    }

    @PostMapping("/{sessionId}/finish")
    @Operation(summary = "结束练习并获取报告")
    public ApiResponse<PracticeService.PracticeReport> finishPractice(
        HttpServletRequest request,
        @PathVariable Long sessionId
    ) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(practiceService.finishPractice(userId, sessionId));
    }

    @GetMapping("/{sessionId}/report")
    @Operation(summary = "查看练习报告")
    public ApiResponse<PracticeService.PracticeReport> getReport(
        HttpServletRequest request,
        @PathVariable Long sessionId
    ) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(practiceService.getSessionReport(userId, sessionId));
    }
}
