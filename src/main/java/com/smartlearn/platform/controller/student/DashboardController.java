package com.smartlearn.platform.controller.student;

import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.dto.DashboardDTO;
import com.smartlearn.platform.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/dashboard")
@Tag(name = "学习数据看板", description = "个人学习数据统计")
@com.smartlearn.platform.interceptor.RequireRole("STUDENT")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "获取学习数据看板")
    public ApiResponse<DashboardDTO> getDashboard(HttpServletRequest request) {
        var userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(dashboardService.getDashboard(userId));
    }
}
