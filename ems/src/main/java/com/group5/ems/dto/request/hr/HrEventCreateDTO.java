package com.group5.ems.dto.request.hr;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record HrEventCreateDTO(
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
