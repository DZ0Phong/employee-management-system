package com.group5.ems.dto.hr;

import java.math.BigDecimal;

/**
 * Summary DTO for the entire payroll run for a specific period.
 */
public record PayrollRunSummaryDTO(
    Long periodId,
    String periodName,
    int totalPayslips,
    BigDecimal companyTotalGross,
    BigDecimal companyTotalNet,
    boolean isFullyApproved,
    String netVarianceIndicator,
    int anomalyCount,
    int negativeNetCount,
    int highOvertimeCount
) {}
