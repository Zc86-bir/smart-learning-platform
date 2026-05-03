package com.smartlearn.platform.service;

import com.smartlearn.platform.entity.DictData;
import com.smartlearn.platform.entity.DictType;

import java.util.List;
import java.util.Map;

public interface DictService {

    List<DictType> listTypes(String keyword);

    DictType createType(DictType type);

    void deleteType(Long id);

    List<DictData> listDataByTypeCode(String typeCode);

    Map<String, List<DictData>> listAllData();

    DictData createData(DictData data);

    DictData updateData(Long id, DictData data);

    void deleteData(Long id);
}
