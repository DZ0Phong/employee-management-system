package com.group5.ems.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record HrPayrollDTO(
    Long id,
    String employeeName,
    String initials,
    String department,
    String position,
    BigDecimal basicSalary,
    BigDecimal totalAllowances,
    BigDecimal totalDeductions,
    BigDecimal netSalary,
    String status,
    LocalDate paymentDate
) {
}
