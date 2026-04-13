package com.group5.ems.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record HrRequestDTO(
    Long id,
    String requestedBy,
    String initials,
    String department,
    Long departmentId,
    String employeeCode,
    String employeePosition,
    String category,
    String categoryCode,
    String title,
    String content,
    String status,
    String rejectedReason,
    LocalDateTime submittedAt,
    String submittedAtDisplay,
    String processedAt,
    String approverName,
    String statusClass,
    String statusDisplay,
    String stepDisplay,
    BigDecimal leaveBalanceRemaining,
    BigDecimal leaveBalanceTotal,
    Integer leaveBalancePercentage,
    Integer overlapCount,
    boolean isLeaveRequest
) {
}

