package com.group5.ems.dto.response.hr;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Aggregated payroll data for a single employee during a timesheet period.
 * Used by the Payroll Preview dashboard.
 */
@Builder
public record EmployeeAggregationDTO(
        Long employeeId,
        String employeeCode,
        String fullName,
        int standardDays,
        double unpaidLeaveDays,
        double payableDays,
        BigDecimal totalOvertimeHours,
        BigDecimal totalBonuses,
        BigDecimal totalDeductions
) {
}
