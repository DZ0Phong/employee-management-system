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
public class EventUpdateDTO {
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
    private Long departmentId;
    private String assignedDepartments; // JSON: ["1","2","3"] (department IDs)
    private Boolean isCompanyWide;      // True = all departments
}