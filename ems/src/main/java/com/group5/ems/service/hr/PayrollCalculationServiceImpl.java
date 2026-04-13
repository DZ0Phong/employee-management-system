package com.group5.ems.service.hr;

import com.group5.ems.dto.response.hr.EmployeeAggregationDTO;
import com.group5.ems.entity.*;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.exception.PayrollPreviewNotFoundException;
import com.group5.ems.repository.*;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollCalculationServiceImpl implements PayrollCalculationService {

    private final TimesheetPeriodRepository periodRepository;
    private final PayslipRepository payslipRepository;
    private final SalaryRepository salaryRepository;
    private final PayComponentRepository payComponentRepository;
    private final PayrollAggregationService aggregationService;
    private final LogService logService;

    @Override
    @Transactional
    public int generateDraftPayslips(Long periodId) {
        // 1. Validation: Check if period exists and is locked
        TimesheetPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new PayrollPreviewNotFoundException(periodId));

        if (!Boolean.TRUE.equals(period.getIsLocked())) {
            throw new IllegalStateException("Period must be locked before generating payslips.");
        }

        // 2. Idempotency Check: Check if payslips already exist for this period
        if (!payslipRepository.findByPeriodId(periodId).isEmpty()) {
            throw new IllegalStateException("Payslips have already been generated for this period.");
        }

        // 3. Data Fetching
        // Get aggregated attendance, OT, and bonuses for all active employees
        List<EmployeeAggregationDTO> aggregations = aggregationService.previewAllEmployees(periodId);
        
        // Map pay components by code for easy access
        Map<String, PayComponent> componentMap = payComponentRepository.findAll().stream()
                .collect(Collectors.toMap(PayComponent::getCode, c -> c));

        // Required components (Mapping to provided database codes)
        PayComponent baseComp = componentMap.get("BASE_SALARY");
        PayComponent otComp = componentMap.get("OT_150");
        PayComponent allowanceComp = componentMap.get("ALLOWANCE"); // Used for bonuses/allowances

        if (baseComp == null || otComp == null) {
             throw new IllegalStateException("Crucial Pay Components (BASE_SALARY, OT_150) are missing. Please configure them in Pay Components settings.");
        }

        List<Payslip> payslipList = new ArrayList<>();

        // 4. In-Memory Math (Gross-to-Net Loop)
        for (EmployeeAggregationDTO agg : aggregations) {
            // Fetch current salary for the employee
            BigDecimal baseSalaryAmount = salaryRepository
                    .findFirstByEmployeeIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                            agg.employeeId(), period.getEndDate())
                    .map(Salary::getBaseAmount)
                    .orElse(BigDecimal.ZERO);

            // Calculate Prorated Base: (Base Salary / Standard Work Days) * Actual Payable Days
            BigDecimal actualBase = BigDecimal.ZERO;
            if (agg.standardDays() > 0) {
                actualBase = baseSalaryAmount
                        .multiply(BigDecimal.valueOf(agg.payableDays()))
                        .divide(BigDecimal.valueOf(agg.standardDays()), 2, RoundingMode.HALF_UP);
            }

            // Calculate Overtime Amount: (Base / Standard / 8 hours) * 1.5 Multiplier * Total OT Hours
            BigDecimal otAmount = BigDecimal.ZERO;
            if (agg.standardDays() > 0 && agg.totalOvertimeHours().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal hourlyRate = baseSalaryAmount
                        .divide(BigDecimal.valueOf(agg.standardDays()), 4, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);
                otAmount = hourlyRate
                        .multiply(BigDecimal.valueOf(1.5))
                        .multiply(agg.totalOvertimeHours())
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // Gross = Base + OT + Bonuses
            BigDecimal totalGross = actualBase.add(otAmount).add(agg.totalBonuses());
            
            // Net = Gross - Deductions (Deductions summarized from aggregations)
            BigDecimal netSalary = totalGross.subtract(agg.totalDeductions());

            // Build Payslip Entity
            Payslip payslip = new Payslip();
            payslip.setEmployeeId(agg.employeeId());
            payslip.setPeriodId(periodId);
            payslip.setActualBaseSalary(actualBase);
            payslip.setTotalOtAmount(otAmount);
            payslip.setTotalBonus(agg.totalBonuses());
            payslip.setTotalDeduction(agg.totalDeductions());
            payslip.setTotalGrossSalary(totalGross);
            payslip.setNetSalary(netSalary);
            payslip.setStatus("PENDING");

            // Build Line Items for transparency
            List<PayslipLineItem> lineItems = new ArrayList<>();
            lineItems.add(createLineItem(payslip, baseComp, "Base Salary (" + agg.payableDays() + " payable days)", actualBase));
            
            if (otAmount.compareTo(BigDecimal.ZERO) > 0) {
                lineItems.add(createLineItem(payslip, otComp, "Overtime Rate 1.5x (" + agg.totalOvertimeHours() + " hours)", otAmount));
            }
            
            if (agg.totalBonuses().compareTo(BigDecimal.ZERO) > 0 && allowanceComp != null) {
                lineItems.add(createLineItem(payslip, allowanceComp, "Period Bonuses & Allowances", agg.totalBonuses()));
            }

            payslip.setLineItems(lineItems);
            payslipList.add(payslip);
        }

        // 5. Batch Save: Hibernate Batch Insert enabled in application.properties
        payslipRepository.saveAll(payslipList);
        
        // Rule #15 Log the batch creation action
        logService.log(AuditAction.CREATE, AuditEntityType.PAYSLIPS, periodId);

        return payslipList.size();
    }

    private PayslipLineItem createLineItem(Payslip payslip, PayComponent pc, String desc, BigDecimal amount) {
        PayslipLineItem item = new PayslipLineItem();
        item.setPayslip(payslip);
        item.setPayComponent(pc);
        item.setDescription(desc);
        item.setAmount(amount);
        item.setType(pc.getType());
        return item;
    }
}
