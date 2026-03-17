package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String type;
    private String color;
    private Boolean isAllDay;
    private String creatorName;
    private String departmentName;

    // Formatted cho hiển thị
    private String monthLabel;   // "MAR"
    private String dayLabel;     // "17"
    private String timeLabel;    // "09:00 AM - 11:00 AM"
    private String colorClass;   // "bg-blue-50 dark:bg-blue-900/20 text-blue-600"
}