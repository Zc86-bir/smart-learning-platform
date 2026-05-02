package com.smartlearn.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlearn.platform.entity.AiUsageLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiUsageMapper extends BaseMapper<AiUsageLog> {
}
