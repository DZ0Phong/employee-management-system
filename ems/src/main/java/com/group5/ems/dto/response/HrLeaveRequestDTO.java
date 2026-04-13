package com.group5.ems.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record HrLeaveRequestDTO(
        Long id,
        String employeeName,
        String initials,
        String department,
        Long departmentId,
        String employeeCode,
        String employeePosition,
        String leaveType,
        String duration,
        String dates,
        String reason,
        LocalDate leave_from,
        LocalDate leave_to,
        String status,
        String statusClass,
        String statusDisplay,
        String stepDisplay,
        String rejectedReason,
        String submittedAtDisplay,
        String processedAt,
        String approverName,
        BigDecimal leaveBalanceRemaining,
        BigDecimal leaveBalanceTotal,
        Integer leaveBalancePercentage,
        Integer overlapCount
) {
}