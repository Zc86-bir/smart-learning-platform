package com.smartlearn.platform.dto;

public record LeaderboardEntry(
    Long userId,
    String nickname,
    int score,
    int durationSeconds,
    String formattedDuration
) {}
