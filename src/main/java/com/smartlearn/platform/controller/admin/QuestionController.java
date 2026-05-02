package com.smartlearn.platform.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.dto.QuestionDTO;
import com.smartlearn.platform.entity.Question;
import com.smartlearn.platform.request.GenerateQuestionsRequest;
import com.smartlearn.platform.service.AiChatService;
import com.smartlearn.platform.service.QuestionService;
import com.smartlearn.platform.service.QuestionImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/questions")
@Tag(name = "题库管理(管理员)", description = "AI出题与题目管理")
@com.smartlearn.platform.interceptor.RequireRole("ADMIN")
public class QuestionController {

    private final QuestionService questionService;
    private final AiChatService aiChatService;
    private final QuestionImportService importService;

    public QuestionController(QuestionService questionService, AiChatService aiChatService, QuestionImportService importService) {
        this.questionService = questionService;
        this.aiChatService = aiChatService;
        this.importService = importService;
    }

    @PostMapping("/import/parse")
    @Operation(summary = "解析导入文件", description = "上传文件并解析为题目预览，支持多文件上传")
    public ApiResponse<List<QuestionDTO>> parseImport(@RequestParam("files") MultipartFile[] files) throws IOException {
        var allQuestions = new java.util.ArrayList<QuestionDTO>();
        for (var file : files) {
            if (file.isEmpty()) continue;
            var extension = file.getOriginalFilename();
            extension = extension != null && extension.contains(".") ? extension.substring(extension.lastIndexOf('.') + 1) : "";
            var questions = importService.parseImport(file.getInputStream(), extension);
            allQuestions.addAll(questions);
        }
        return ApiResponse.ok(allQuestions);
    }

    @PostMapping("/import/save")
    @Operation(summary = "保存导入的题目", description = "批量保存已确认的题目")
    public ApiResponse<Map<String, Integer>> saveImport(@RequestBody List<QuestionDTO> questions) {
        int saved = importService.saveImported(questions);
        return ApiResponse.ok(Map.of("saved", saved, "total", questions.size()));
    }

    @PostMapping("/generate")
    @Operation(summary = "AI智能出题")
    public ApiResponse<List<QuestionDTO>> generate(@Valid @RequestBody GenerateQuestionsRequest request) {
        return ApiResponse.ok(questionService.generateQuestions(request));
    }

    @PostMapping
    @Operation(summary = "保存题目")
    public ApiResponse<QuestionDTO> save(@RequestBody Question question) {
        return ApiResponse.ok(questionService.saveQuestion(question));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取题目详情")
    public ApiResponse<QuestionDTO> getById(@PathVariable Long id) {
        return ApiResponse.ok(questionService.getQuestionById(id));
    }

    @GetMapping
    @Operation(summary = "分页查询题目")
    public ApiResponse<Page<QuestionDTO>> list(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String difficulty,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(questionService.listQuestions(category, difficulty, keyword, page, size));
    }

    @GetMapping("/hot")
    @Operation(summary = "热门题目")
    public ApiResponse<List<QuestionDTO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(questionService.getHotQuestions(limit));
    }

    @GetMapping("/models")
    @Operation(summary = "可用模型列表")
    public ApiResponse<List<String>> listModels() throws Exception {
        return ApiResponse.ok(aiChatService.listModels().get());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新题目")
    public ApiResponse<QuestionDTO> update(@PathVariable Long id, @RequestBody Question question) {
        return ApiResponse.ok(questionService.updateQuestion(id, question));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除题目", description = "按ID列表批量删除题目")
    public ApiResponse<Map<String, Integer>> batchDelete(@RequestBody List<Long> ids) {
        int deleted = questionService.batchDeleteQuestions(ids);
        return ApiResponse.ok(Map.of("deleted", deleted, "total", ids.size()));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "删除题目")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/stats")
    @Operation(summary = "分类统计")
    public ApiResponse<java.util.Map<String, Long>> stats() {
        return ApiResponse.ok(questionService.getCategoryStats());
    }

    @GetMapping("/template")
    @Operation(summary = "下载导入模板", description = "下载标准的 Excel 导入模板文件")
    @com.smartlearn.platform.interceptor.PermitAll
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"question_import_template.xlsx\"");

        try (var workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(true)) {
            var sheet = workbook.createSheet("题目模板");
            var headerRow = sheet.createRow(0);
            String[] headers = {"题型", "分类", "题干", "选项A", "选项B", "选项C", "选项D", "答案", "解析", "难度", "知识点"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            var sampleRow = sheet.createRow(1);
            String[] sample = {"单选题", "数学", "函数 f(x) = x^2 + 1 的导数是？", "2x", "2x+1", "x^2", "1", "A", "幂函数求导公式", "中等", "导数"};
            for (int i = 0; i < sample.length; i++) {
                sampleRow.createCell(i).setCellValue(sample[i]);
            }
            for (int i = 0; i < Math.min(headers.length, 6); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(response.getOutputStream());
        }
    }
}
