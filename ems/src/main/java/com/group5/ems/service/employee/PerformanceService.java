package com.group5.ems.service.employee;

import com.group5.ems.dto.response.PerformanceReviewDTO;
import com.group5.ems.dto.response.PerformanceSummaryDTO;

import java.util.List;

public interface PerformanceService {
    PerformanceSummaryDTO getPerformanceSummary(Long employeeId);
    List<PerformanceReviewDTO> getReviewHistory(Long employeeId);
}