package com.group5.ems.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record HrPerformanceDTO(
    Long id,
    String employeeName,
    String employeeCode,
    String avatarUrl,
    String department,
    String reviewerName,
    String reviewerCode,
    String status,
    String performanceGrade,
    String potentialGrade,
    String reviewPeriod,
    BigDecimal performanceScore,
    BigDecimal potentialScore,
    String strengths,
    String areasToImprove,
    LocalDateTime createdAt
) {
}
