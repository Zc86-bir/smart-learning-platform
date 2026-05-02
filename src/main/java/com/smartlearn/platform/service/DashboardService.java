package com.smartlearn.platform.service;

import com.smartlearn.platform.dto.DashboardDTO;

public interface DashboardService {
    /**
     * Get student's learning dashboard data.
     */
    DashboardDTO getDashboard(Long userId);
}
