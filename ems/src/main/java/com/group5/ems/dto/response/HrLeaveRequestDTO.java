package com.group5.ems.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record HrLeaveRequestDTO(
    Long id,
    String employeeName,
    String initials,
    String department,
    String leaveType,
    String duration,
    LocalDate startDate,
    LocalDate endDate,
    String status
) {
}
