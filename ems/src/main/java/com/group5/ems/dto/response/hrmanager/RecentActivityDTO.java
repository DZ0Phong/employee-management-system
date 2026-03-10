package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDTO {
    private Long id;
    private String employeeName;
    private String employeePosition;
    private String employeeInitials;
    private String actionLabel;
    private LocalDate date;
    private String status; // "PENDING", "APPROVED", "PROCESSING"
    private String statusLabel;
}