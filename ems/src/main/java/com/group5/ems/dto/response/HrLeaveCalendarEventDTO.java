package com.group5.ems.dto.response;

import lombok.Builder;

/**
 * FullCalendar-compatible event DTO for team leave calendar view.
 */
@Builder
public record HrLeaveCalendarEventDTO(
        Long id,
        String title,
        String start,
        String end,
        String color,
        String textColor,
        String borderColor,
        String department,
        String leaveType,
        String status
) {
}
