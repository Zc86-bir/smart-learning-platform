package com.smartlearn.platform.service;

import com.smartlearn.platform.dto.GradingReport;
import com.smartlearn.platform.dto.LeaderboardEntry;
import com.smartlearn.platform.entity.ExamRecord;

import java.util.List;
import java.util.Map;

public interface ExamService {
    ExamRecord startExam(Long userId, Long paperId);
    void submitAnswer(Long userId, Long examRecordId, String answersJson, int cutScreenCount, int clipboardCount);
    void forceSubmit(Long examRecordId, String reason);
    void heartbeat(Long userId, Long examRecordId);
    void reportSuspicious(Long userId, Long examRecordId, String flagType, String detail);
    GradingReport getGradingReport(Long examRecordId);
    List<LeaderboardEntry> getLeaderboard(Long paperId, int limit);
    Map<String, Object> getAntiCheatStatus(Long userId, Long examRecordId);
}
