package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDTO {

    private Long id;
    
    // Activity type & classification
    private String activityType;        // LEAVE, PAYROLL, STATUS_CHANGE, HR_REQUEST
    
    // Employee info
    private String employeeName;
    private String employeePosition;
    private String employeeInitials;
    private String department;          // Department name
    
    // Activity details
    private String actionLabel;         // "Leave Request", "Payroll", "New Hire", etc.
    private String details;             // "Annual Leave: 5 days", "Net Salary: $5,000", etc.
    private LocalDate date;
    
    // Status & priority
    private String status;              // PENDING, APPROVED, REJECTED, COMPLETED
    private String statusLabel;         // "Chờ duyệt", "Đã duyệt", etc.
    private String priority;            // URGENT, NORMAL, LOW
    
    // UI styling
    private String icon;                // calendar_today, payments, person, description
    private String color;               // blue, yellow, purple, green, red
    private String badge;               // "New hire", "Promotion", "Termination"
}
