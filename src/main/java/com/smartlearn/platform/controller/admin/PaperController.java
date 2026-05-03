package com.smartlearn.platform.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.annotation.LogOperation;
import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.dto.PaperDTO;
import com.smartlearn.platform.dto.QuestionWithScoreDTO;
import com.smartlearn.platform.entity.Paper;
import com.smartlearn.platform.request.SmartPaperRequest;
import com.smartlearn.platform.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/papers")
@Tag(name = "试卷管理(管理员)", description = "智能组卷与试卷管理")
@com.smartlearn.platform.interceptor.RequireRole("ADMIN")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @PostMapping("/smart-generate")
    @Operation(summary = "AI智能组卷")
    @LogOperation(module = "paper", operation = "AI_GENERATE")
    public ApiResponse<List<QuestionWithScoreDTO>> smartGenerate(@Valid @RequestBody SmartPaperRequest request) {
        return ApiResponse.ok(paperService.smartGenerate(request));
    }

    @PostMapping
    @Operation(summary = "创建试卷")
    @LogOperation(module = "paper", operation = "CREATE")
    public ApiResponse<PaperDTO> create(@RequestBody Paper paper) {
        return ApiResponse.ok(paperService.createPaper(paper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取试卷详情")
    public ApiResponse<PaperDTO> getById(@PathVariable Long id) {
        return ApiResponse.ok(paperService.getPaperDTO(id));
    }

    @GetMapping
    @Operation(summary = "分页查询试卷")
    public ApiResponse<Page<Paper>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(paperService.listPapers(page, size));
    }
}
