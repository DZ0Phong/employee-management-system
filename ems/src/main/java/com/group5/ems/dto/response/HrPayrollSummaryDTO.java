package com.group5.ems.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record HrPayrollSummaryDTO(
        BigDecimal totalNetSalary,
        BigDecimal totalBaseSalary,
        BigDecimal totalDeductions,
        long employeeCount
) {
}