package com.group5.ems.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record HrLeaveRequestDTO(
    Long id,
    String employeeName,
    String initials,
    String department,
    String employeeCode,
    String leaveType,
    String duration,
    String dates,
    String reason,
    LocalDate startDate,
    LocalDate endDate,
    String status
) {
}
