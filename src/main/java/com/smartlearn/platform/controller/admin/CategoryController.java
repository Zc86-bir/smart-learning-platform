package com.smartlearn.platform.controller.admin;

import com.smartlearn.platform.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
@Tag(name = "分类管理(管理员)", description = "分类CRUD")
@com.smartlearn.platform.interceptor.RequireRole("ADMIN")
public class CategoryController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CategoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @Operation(summary = "分类列表")
    public ApiResponse<List<Map<String, Object>>> list() {
        var rows = jdbcTemplate.queryForList(
            "SELECT id, name, parent_id, sort_order FROM categories WHERE deleted = 0 ORDER BY sort_order");
        return ApiResponse.ok(rows);
    }

    @PostMapping
    @Operation(summary = "新增分类")
    public ApiResponse<Void> create(@RequestParam String name,
                                    @RequestParam(required = false) Long parentId,
                                    @RequestParam(defaultValue = "0") int sortOrder) {
        jdbcTemplate.update(
            "INSERT INTO categories (name, parent_id, sort_order) VALUES (?, ?, ?)",
            name, parentId, sortOrder);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE categories SET deleted = 1 WHERE id = ?", id);
        return ApiResponse.ok();
    }
}
