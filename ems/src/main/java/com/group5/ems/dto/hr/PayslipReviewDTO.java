package com.group5.ems.dto.hr;

import java.math.BigDecimal;

/**
 * Lightweight DTO for the Payroll Review Dashboard table.
 */
public record PayslipReviewDTO(
    Long payslipId,
    String employeeCode,
    String fullName,
    BigDecimal totalGross,
    BigDecimal totalDeductions,
    BigDecimal netSalary,
    String status
) {}
