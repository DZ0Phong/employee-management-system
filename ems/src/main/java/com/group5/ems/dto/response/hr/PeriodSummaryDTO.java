package com.group5.ems.dto.response.hr;

import lombok.Builder;

import java.time.LocalDate;

/**
 * Summary metrics for a timesheet period, used by the Payroll Preview dashboard cards.
 */
@Builder
public record PeriodSummaryDTO(
        Long periodId,
        String periodName,
        boolean locked,
        LocalDate startDate,
        LocalDate endDate,
        int totalEmployeesProcessed,
        double avgPayableDays
) {
}
