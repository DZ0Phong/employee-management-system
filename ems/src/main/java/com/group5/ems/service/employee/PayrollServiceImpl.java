package com.group5.ems.service.employee;

import com.group5.ems.dto.response.PayrollSummaryDTO;
import com.group5.ems.dto.response.PayslipDTO;
import com.group5.ems.dto.response.BankDetailsResponseDTO;
import com.group5.ems.entity.Payslip;
import com.group5.ems.entity.Salary;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.PayslipRepository;
import com.group5.ems.repository.SalaryRepository;
import com.group5.ems.service.common.LogService;
import com.group5.ems.service.employee.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayslipRepository payslipRepository;
    private final SalaryRepository salaryRepository;
    private final LogService logService;

    @Override
    @Transactional(readOnly = true)
    public PayrollSummaryDTO getPayrollSummary(Long employeeId) {
        // Lấy payslip mới nhất
        Optional<Payslip> latestPayslip = payslipRepository.findTopByEmployeeIdOrderByIdDesc(employeeId);

        // Lấy salary hiện tại
        Optional<Salary> currentSalary = salaryRepository.findTopByEmployeeIdOrderByEffectiveFromDesc(employeeId);

        String periodName = "";
        BigDecimal grossPay = BigDecimal.ZERO;
        BigDecimal deduction = BigDecimal.ZERO;
        BigDecimal netPay = BigDecimal.ZERO;

        if (latestPayslip.isPresent()) {
            Payslip p = latestPayslip.get();
            periodName = p.getPeriod() != null ? p.getPeriod().getPeriodName() : "N/A";
            grossPay = mapToDTO(p).getGrossPay();
            deduction = p.getTotalDeduction() != null ? p.getTotalDeduction() : BigDecimal.ZERO;
            netPay = p.getNetSalary() != null ? p.getNetSalary() : BigDecimal.ZERO;
        }

        BigDecimal baseSalary = BigDecimal.ZERO;
        BigDecimal allowance = BigDecimal.ZERO;
        if (currentSalary.isPresent()) {
            baseSalary = currentSalary.get().getBaseAmount();
            allowance = currentSalary.get().getAllowanceAmount() != null
                    ? currentSalary.get().getAllowanceAmount() : BigDecimal.ZERO;
        }

        return PayrollSummaryDTO.builder()
                .latestPeriodName(periodName)
                .latestGrossPay(grossPay)
                .latestDeduction(deduction)
                .latestNetPay(netPay)
                .currentBaseSalary(baseSalary)
                .currentAllowance(allowance)
                .hasBankDetails(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayslipDTO> getPayslipHistory(Long employeeId) {
        return payslipRepository.findByEmployeeIdOrderByIdDesc(employeeId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportPayslipCsv(Long employeeId) {
        List<PayslipDTO> payslips = getPayslipHistory(employeeId);
        logService.log(AuditAction.ACCESS, AuditEntityType.PAYROLL, employeeId);

        StringBuilder csv = new StringBuilder();
        csv.append("Period,Gross Pay,Deductions,Net Pay,Status\n");
        for (PayslipDTO p : payslips) {
            csv.append(p.getPeriodName()).append(",")
                    .append(p.getGrossPay()).append(",")
                    .append(p.getTotalDeduction() != null ? p.getTotalDeduction() : "0").append(",")
                    .append(p.getNetSalary() != null ? p.getNetSalary() : "0").append(",")
                    .append(p.getStatus()).append("\n");
        }
        return csv.toString().getBytes();
    }

    @Override
    public void applyPrimaryBankDetail(PayrollSummaryDTO summary, BankDetailsResponseDTO primaryBankDetail) {
        if (summary == null) {
            return;
        }
        if (primaryBankDetail == null) {
            summary.setHasBankDetails(false);
            summary.setPrimaryBankName(null);
            summary.setPrimaryAccountName(null);
            summary.setPrimaryMaskedAccountNumber(null);
            return;
        }
        summary.setHasBankDetails(true);
        summary.setPrimaryBankName(primaryBankDetail.bankName());
        summary.setPrimaryAccountName(primaryBankDetail.accountName());
        summary.setPrimaryMaskedAccountNumber(primaryBankDetail.maskedAccountNumber());
    }

    // ── Helper ─────────────────────────────────────────────
    private PayslipDTO mapToDTO(Payslip p) {
        String periodName = p.getPeriod() != null ? p.getPeriod().getPeriodName() : "N/A";

        return PayslipDTO.builder()
                .id(p.getId())
                .periodName(periodName)
                .actualBaseSalary(p.getActualBaseSalary())
                .totalOtAmount(p.getTotalOtAmount())
                .totalBonus(p.getTotalBonus())
                .totalDeduction(p.getTotalDeduction())
                .netSalary(p.getNetSalary())
                .status(p.getStatus())
                .build();
    }
}
