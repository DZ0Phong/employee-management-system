package com.group5.ems.dto.response.hr;

import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record HrEventResponseDTO(
    Long id,
    String title,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime,
    String type,
    String color,
    Boolean isAllDay,
    Long departmentId
) {}
