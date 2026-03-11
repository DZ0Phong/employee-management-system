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
    private String employeeName;
    private String employeePosition;
    private String employeeInitials;
    private String actionLabel;
    private LocalDate date;
    private String status;
    private String statusLabel;  // ví dụ: "PENDING" → "Chờ duyệt"
}