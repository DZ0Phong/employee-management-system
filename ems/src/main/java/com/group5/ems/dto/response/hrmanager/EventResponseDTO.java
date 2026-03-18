package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String startDate;      // "2026-03-18"
    private String endDate;
    private String startTime;      // "09:00"
    private String endTime;
    private String type;
    private String color;
    private Boolean isAllDay;
    private String creatorName;
    private String departmentName;
    private String monthLabel;     // "MAR"
    private String dayLabel;       // "18"
    private String timeLabel;      // "09:00 AM - 11:00 AM"
    private String colorClass;     // "bg-blue-50 text-blue-600"
}