package com.group5.ems.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Leave balance summary for the current year, aggregated across all employees.
 */
@Builder
public record HrLeaveBalanceSummaryDTO(
        BigDecimal totalDaysAllocated,
        BigDecimal totalDaysUsed,
        BigDecimal totalDaysPending,
        BigDecimal totalDaysRemaining,
        long employeesWithBalance,
        double avgUtilizationPercent
) {
}
