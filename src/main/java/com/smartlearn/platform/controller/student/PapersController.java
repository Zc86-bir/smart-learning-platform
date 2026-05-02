package com.smartlearn.platform.controller.student;

import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.entity.Paper;
import com.smartlearn.platform.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/papers")
@Tag(name = "学生试卷", description = "试卷列表与详情")
@com.smartlearn.platform.interceptor.RequireRole("STUDENT")
public class PapersController {

    private final PaperService paperService;

    public PapersController(PaperService paperService) {
        this.paperService = paperService;
    }

    @GetMapping
    @Operation(summary = "试卷列表")
    public ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Paper>> listPapers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(paperService.listPapers(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "试卷详情")
    public ApiResponse<Paper> getPaper(@PathVariable Long id) {
        return ApiResponse.ok(paperService.getPaperById(id));
    }
}
