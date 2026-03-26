package com.group5.ems.dto.hr;

import java.math.BigDecimal;

/**
 * Lightweight DTO for the Payroll Review Dashboard table.
 */
public record PayslipReviewDTO(
    Long payslipId,
    String employeeCode,
    String fullName,
    BigDecimal actualBaseSalary,
    BigDecimal totalGross,
    BigDecimal totalDeductions,
    BigDecimal netSalary,
    String status,
    BigDecimal totalOtAmount,
    BigDecimal totalBonus,
    boolean isNegativeNet,
    boolean isHighOvertime
) {
    // Constructor for JPQL binding
    public PayslipReviewDTO(Long payslipId, String employeeCode, String fullName, 
                            BigDecimal actualBaseSalary, BigDecimal totalGross, 
                            BigDecimal totalDeductions, BigDecimal netSalary, String status, 
                            BigDecimal totalOtAmount, BigDecimal totalBonus) {
        this(payslipId, employeeCode, fullName, actualBaseSalary, totalGross, totalDeductions, netSalary, status, totalOtAmount, totalBonus, false, false);
    }
}
