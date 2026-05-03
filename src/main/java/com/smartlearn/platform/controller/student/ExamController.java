package com.smartlearn.platform.controller.student;

import com.smartlearn.platform.annotation.Idempotent;
import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.dto.GradingReport;
import com.smartlearn.platform.dto.LeaderboardEntry;
import com.smartlearn.platform.entity.ExamRecord;
import com.smartlearn.platform.request.AntiCheatReportRequest;
import com.smartlearn.platform.request.SubmitExamRequest;
import com.smartlearn.platform.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/exams")
@Tag(name = "学生考试", description = "考试相关接口")
@com.smartlearn.platform.interceptor.RequireRole("STUDENT")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping("/start/{paperId}")
    @Operation(summary = "开始考试")
    public ApiResponse<ExamRecord> startExam(HttpServletRequest request, @PathVariable Long paperId) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(examService.startExam(userId, paperId));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交答卷")
    @Idempotent(prefix = "exam:submit:", ttlSeconds = 600, message = "请勿重复提交答卷")
    public ApiResponse<Void> submitExam(HttpServletRequest request, @Valid @RequestBody SubmitExamRequest req) {
        var userId = (Long) request.getAttribute("userId");
        examService.submitAnswer(userId, req.examRecordId(), req.answers(),
            req.cutScreenCount() != null ? req.cutScreenCount() : 0,
            req.clipboardCount() != null ? req.clipboardCount() : 0);
        return ApiResponse.ok();
    }

    @GetMapping("/{examRecordId}/report")
    @Operation(summary = "获取AI判卷报告")
    public ApiResponse<GradingReport> getReport(@PathVariable Long examRecordId) {
        return ApiResponse.ok(examService.getGradingReport(examRecordId));
    }

    @GetMapping("/leaderboard/{paperId}")
    @Operation(summary = "获取排行榜")
    public ApiResponse<List<LeaderboardEntry>> getLeaderboard(
        @PathVariable Long paperId,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(examService.getLeaderboard(paperId, limit));
    }

    @PostMapping("/heartbeat/{examRecordId}")
    @Operation(summary = "心跳保活（每30秒调用一次）")
    public ApiResponse<Void> heartbeat(HttpServletRequest request, @PathVariable Long examRecordId) {
        var userId = (Long) request.getAttribute("userId");
        examService.heartbeat(userId, examRecordId);
        return ApiResponse.ok();
    }

    @PostMapping("/report-suspicious")
    @Operation(summary = "前端上报可疑行为")
    public ApiResponse<Void> reportSuspicious(HttpServletRequest request, @Valid @RequestBody AntiCheatReportRequest req) {
        var userId = (Long) request.getAttribute("userId");
        if (req.cutScreenCount() != null || req.clipboardCount() != null) {
            examService.submitAnswer(userId, req.examRecordId(), "{}",
                req.cutScreenCount() != null ? req.cutScreenCount() : 0,
                req.clipboardCount() != null ? req.clipboardCount() : 0);
        }
        return ApiResponse.ok();
    }

    @GetMapping("/anticheat/{examRecordId}")
    @Operation(summary = "获取防作弊状态")
    public ApiResponse<Map<String, Object>> getAntiCheatStatus(
        HttpServletRequest request,
        @PathVariable Long examRecordId
    ) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(examService.getAntiCheatStatus(userId, examRecordId));
    }
}
