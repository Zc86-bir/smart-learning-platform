package com.smartlearn.platform.controller.admin;

import com.smartlearn.platform.annotation.LogOperation;
import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.entity.DictData;
import com.smartlearn.platform.entity.DictType;
import com.smartlearn.platform.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dict")
@Tag(name = "数据字典管理", description = "系统数据字典配置")
@com.smartlearn.platform.interceptor.RequireRole("ADMIN")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/types")
    @Operation(summary = "字典类型列表")
    public ApiResponse<List<DictType>> listTypes(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(dictService.listTypes(keyword));
    }

    @PostMapping("/types")
    @Operation(summary = "创建字典类型")
    @LogOperation(module = "dict", operation = "CREATE_TYPE")
    public ApiResponse<DictType> createType(@RequestBody DictType type) {
        return ApiResponse.ok(dictService.createType(type));
    }

    @DeleteMapping("/types/{id}")
    @Operation(summary = "删除字典类型")
    @LogOperation(module = "dict", operation = "DELETE_TYPE")
    public ApiResponse<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/data/{typeCode}")
    @Operation(summary = "按类型编码查询字典数据")
    public ApiResponse<List<DictData>> listDataByTypeCode(@PathVariable String typeCode) {
        return ApiResponse.ok(dictService.listDataByTypeCode(typeCode));
    }

    @GetMapping("/data")
    @Operation(summary = "全部字典数据")
    public ApiResponse<Map<String, List<DictData>>> listAllData() {
        return ApiResponse.ok(dictService.listAllData());
    }

    @PostMapping("/data")
    @Operation(summary = "创建字典数据")
    @LogOperation(module = "dict", operation = "CREATE_DATA")
    public ApiResponse<DictData> createData(@RequestBody DictData data) {
        return ApiResponse.ok(dictService.createData(data));
    }

    @PutMapping("/data/{id}")
    @Operation(summary = "更新字典数据")
    @LogOperation(module = "dict", operation = "UPDATE_DATA")
    public ApiResponse<DictData> updateData(@PathVariable Long id, @RequestBody DictData data) {
        return ApiResponse.ok(dictService.updateData(id, data));
    }

    @DeleteMapping("/data/{id}")
    @Operation(summary = "删除字典数据")
    @LogOperation(module = "dict", operation = "DELETE_DATA")
    public ApiResponse<Void> deleteData(@PathVariable Long id) {
        dictService.deleteData(id);
        return ApiResponse.ok(null);
    }
}
