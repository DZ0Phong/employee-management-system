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
public class PayslipDTO {
    private Long id;
    private String periodName;      // VD: "October 2023"
    private BigDecimal actualBaseSalary;
    private BigDecimal totalOtAmount;
    private BigDecimal totalBonus;
    private BigDecimal totalDeduction;
    private BigDecimal netSalary;
    private String status;

    // Gross = base + OT + bonus
    public BigDecimal getGrossPay() {
        BigDecimal base = actualBaseSalary != null ? actualBaseSalary : BigDecimal.ZERO;
        BigDecimal ot = totalOtAmount != null ? totalOtAmount : BigDecimal.ZERO;
        BigDecimal bonus = totalBonus != null ? totalBonus : BigDecimal.ZERO;
        return base.add(ot).add(bonus);
    }
}