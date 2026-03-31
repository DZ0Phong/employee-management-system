package com.group5.ems.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record HrLeaveRequestDTO(
        Long id,
        String employeeName,
        String initials,
        String department,
        Long departmentId,
        String employeeCode,
        String leaveType,
        String duration,
        String dates,
        String reason,
        LocalDate leave_from,
        LocalDate leave_to,
        String status,
        String rejectedReason,
        String processedAt,
        String approverName
) {
}