package com.smartlearn.platform.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.entity.ExamRecord;
import com.smartlearn.platform.mapper.ExamRecordMapper;
import com.smartlearn.platform.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/students")
@Tag(name = "管理后台-学生管理", description = "学生管理接口")
@com.smartlearn.platform.interceptor.RequireRole("ADMIN")
public class AdminStudentController {

    private final UserMapper userMapper;
    private final ExamRecordMapper examRecordMapper;

    public AdminStudentController(UserMapper userMapper, ExamRecordMapper examRecordMapper) {
        this.userMapper = userMapper;
        this.examRecordMapper = examRecordMapper;
    }

    @GetMapping
    @Operation(summary = "学生列表")
    public ApiResponse<Map<String, Object>> listStudents(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword
    ) {
        var students = userMapper.findStudentsWithStats();

        // Filter by keyword
        if (keyword != null && !keyword.isBlank()) {
            students = students.stream()
                .filter(s -> {
                    var nick = (String) s.get("nickname");
                    var user = (String) s.get("username");
                    return (nick != null && nick.contains(keyword)) || (user != null && user.contains(keyword));
                })
                .toList();
        }

        // Manual pagination
        int total = students.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        var pageList = students.subList(Math.max(0, from), Math.max(0, to));

        var result = new HashMap<String, Object>();
        result.put("records", pageList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}/exams")
    @Operation(summary = "学生考试记录")
    public ApiResponse<Page<ExamRecord>> getStudentExams(
        @PathVariable Long id,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRecord>();
        wrapper.eq(ExamRecord::getUserId, id)
            .orderByDesc(ExamRecord::getCreatedAt);
        return ApiResponse.ok(examRecordMapper.selectPage(new Page<>(page, size), wrapper));
    }
}
