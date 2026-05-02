package com.smartlearn.platform.controller.admin;

import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.mapper.ExamRecordMapper;
import com.smartlearn.platform.mapper.QuestionMapper;
import com.smartlearn.platform.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "管理后台-数据看板", description = "数据看板接口")
@com.smartlearn.platform.interceptor.RequireRole("ADMIN")
public class AdminDashboardController {

    private final UserMapper userMapper;
    private final ExamRecordMapper examRecordMapper;
    private final QuestionMapper questionMapper;

    public AdminDashboardController(UserMapper userMapper, ExamRecordMapper examRecordMapper, QuestionMapper questionMapper) {
        this.userMapper = userMapper;
        this.examRecordMapper = examRecordMapper;
        this.questionMapper = questionMapper;
    }

    @GetMapping("/stats")
    @Operation(summary = "获取看板统计")
    public ApiResponse<Map<String, Object>> getStats() {
        var stats = new HashMap<String, Object>();
        stats.put("totalStudents", userMapper.countStudents());
        stats.put("totalQuestions", questionMapper.selectCount(null));
        stats.put("totalExams", examRecordMapper.countTotalExams());
        stats.put("completedExams", examRecordMapper.countCompletedExams());
        stats.put("categoryStats", questionMapper.countByCategory());
        return ApiResponse.ok(stats);
    }

    @GetMapping("/trend")
    @Operation(summary = "考试趋势（近30天）")
    public ApiResponse<List<Map<String, Object>>> examTrend() {
        return ApiResponse.ok(examRecordMapper.examTrendLast30Days());
    }

    @GetMapping("/papers/top")
    @Operation(summary = "热门试卷排行")
    public ApiResponse<List<Map<String, Object>>> topPapers() {
        return ApiResponse.ok(examRecordMapper.topPapers());
    }

    @GetMapping("/monthly")
    @Operation(summary = "月度考试统计")
    public ApiResponse<List<Map<String, Object>>> monthlyStats() {
        return ApiResponse.ok(examRecordMapper.monthlyExamStats());
    }
}
