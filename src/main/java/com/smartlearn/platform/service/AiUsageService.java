package com.smartlearn.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlearn.platform.entity.AiUsageLog;
import com.smartlearn.platform.mapper.AiUsageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AiUsageService {

    private static final Logger log = LoggerFactory.getLogger(AiUsageService.class);

    private final AiUsageMapper aiUsageMapper;

    public AiUsageService(AiUsageMapper aiUsageMapper) {
        this.aiUsageMapper = aiUsageMapper;
    }

    public void log(AiUsageLog aiUsageLog) {
        try {
            aiUsageMapper.insert(aiUsageLog);
        } catch (Exception e) {
            log.warn("[AiUsage] Failed to persist usage log", e);
        }
    }

    public Map<String, Object> getStatsByPurpose(LocalDateTime since) {
        var wrapper = new LambdaQueryWrapper<AiUsageLog>()
            .ge(since != null, AiUsageLog::getCreatedAt, since);

        var all = aiUsageMapper.selectList(wrapper);
        return Map.of(
            "totalCalls", all.size(),
            "totalTokens", all.stream().mapToInt(AiUsageLog::getTotalTokens).sum(),
            "totalDurationMs", all.stream().mapToInt(AiUsageLog::getDurationMs).sum(),
            "failedCalls", (int) all.stream().filter(r -> "FAILED".equals(r.getStatus())).count()
        );
    }

    public Map<String, Long> getModelUsageCount(LocalDateTime since) {
        var wrapper = new LambdaQueryWrapper<AiUsageLog>()
            .select(AiUsageLog::getModel)
            .ge(since != null, AiUsageLog::getCreatedAt, since);

        return aiUsageMapper.selectList(wrapper).stream()
            .collect(java.util.stream.Collectors.groupingBy(AiUsageLog::getModel,
                java.util.stream.Collectors.counting()));
    }
}
