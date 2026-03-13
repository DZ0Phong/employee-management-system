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
public class PayrollRunDTO {
    private Long id;
    private String departmentName;
    private String periodLabel;        // "Monthly Payroll - June 2024"
    private int employeeCount;
    private String totalAmountFormatted; // "€128,450.00"
    private String status;             // PENDING_REVIEW, PROCESSING, APPROVED, REJECTED
    private LocalDate dueDate;
}