package com.group5.ems.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record HrEmployeePerformanceDTO(
        Long id,
        String reviewPeriod,
        BigDecimal performanceScore,
        BigDecimal potentialScore,
        String status,
        String reviewerName,
        LocalDateTime createdAt
) {
}
