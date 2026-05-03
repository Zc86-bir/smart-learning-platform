package com.smartlearn.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlearn.platform.entity.DictData;
import com.smartlearn.platform.entity.DictType;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.DictDataMapper;
import com.smartlearn.platform.mapper.DictTypeMapper;
import com.smartlearn.platform.service.DictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DictServiceImpl implements DictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;

    public DictServiceImpl(DictTypeMapper dictTypeMapper, DictDataMapper dictDataMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
    }

    @Override
    public List<DictType> listTypes(String keyword) {
        var wrapper = new LambdaQueryWrapper<DictType>()
                .eq(DictType::getDeleted, 0)
                .orderByAsc(DictType::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DictType::getName, keyword).or().like(DictType::getCode, keyword));
        }
        return dictTypeMapper.selectList(wrapper);
    }

    @Override
    public DictType createType(DictType type) {
        var existing = dictTypeMapper.selectOne(new LambdaQueryWrapper<DictType>()
                .eq(DictType::getCode, type.getCode()).eq(DictType::getDeleted, 0));
        if (existing != null) {
            throw new BizException("字典编码已存在: " + type.getCode());
        }
        type.setStatus(type.getStatus() != null ? type.getStatus() : 1);
        type.setDeleted(0);
        dictTypeMapper.insert(type);
        return type;
    }

    @Override
    public void deleteType(Long id) {
        dictTypeMapper.deleteById(id);
        dictDataMapper.delete(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictTypeId, id));
    }

    @Override
    public List<DictData> listDataByTypeCode(String typeCode) {
        var type = dictTypeMapper.selectOne(new LambdaQueryWrapper<DictType>()
                .eq(DictType::getCode, typeCode).eq(DictType::getDeleted, 0));
        if (type == null) return List.of();
        return dictDataMapper.selectList(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictTypeId, type.getId())
                .eq(DictData::getDeleted, 0)
                .orderByAsc(DictData::getSort));
    }

    @Override
    public Map<String, List<DictData>> listAllData() {
        var types = dictTypeMapper.selectList(new LambdaQueryWrapper<DictType>()
                .eq(DictType::getDeleted, 0));
        Map<String, List<DictData>> result = new LinkedHashMap<>();
        for (var type : types) {
            var dataList = dictDataMapper.selectList(new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getDictTypeId, type.getId())
                    .eq(DictData::getDeleted, 0)
                    .eq(DictData::getStatus, 1)
                    .orderByAsc(DictData::getSort));
            result.put(type.getCode(), dataList);
        }
        return result;
    }

    @Override
    public DictData createData(DictData data) {
        data.setStatus(data.getStatus() != null ? data.getStatus() : 1);
        data.setDeleted(0);
        data.setSort(data.getSort() != null ? data.getSort() : 0);
        data.setIsDefault(data.getIsDefault() != null ? data.getIsDefault() : 0);
        dictDataMapper.insert(data);
        return data;
    }

    @Override
    public DictData updateData(Long id, DictData data) {
        var existing = dictDataMapper.selectById(id);
        if (existing == null) throw new BizException("字典数据不存在");
        if (data.getLabel() != null) existing.setLabel(data.getLabel());
        if (data.getValue() != null) existing.setValue(data.getValue());
        if (data.getSort() != null) existing.setSort(data.getSort());
        if (data.getColor() != null) existing.setColor(data.getColor());
        if (data.getStatus() != null) existing.setStatus(data.getStatus());
        if (data.getRemark() != null) existing.setRemark(data.getRemark());
        dictDataMapper.updateById(existing);
        return existing;
    }

    @Override
    public void deleteData(Long id) {
        dictDataMapper.deleteById(id);
    }
}
