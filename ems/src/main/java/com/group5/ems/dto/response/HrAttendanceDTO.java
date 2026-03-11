package com.group5.ems.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record HrAttendanceDTO(
    Long id,
    String employeeName,
    String initials,
    String department,
    LocalDate workDate,
    LocalTime checkIn,
    LocalTime checkOut,
    String status
) {
}
