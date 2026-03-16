package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollSummaryDTO {
    // Latest payslip info
    private String latestPeriodName;
    private BigDecimal latestGrossPay;
    private BigDecimal latestDeduction;
    private BigDecimal latestNetPay;

    // Current salary info
    private BigDecimal currentBaseSalary;
    private BigDecimal currentAllowance;
}