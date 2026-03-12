package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrPayrollDTO;
import com.group5.ems.dto.response.HrPayrollSummaryDTO;
import com.group5.ems.entity.Payslip;
import com.group5.ems.repository.PayslipRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HrPayrollService {

    private final PayslipRepository payslipRepository;

    public HrPayrollService(PayslipRepository payslipRepository) {
        this.payslipRepository = payslipRepository;
    }

    public List<HrPayrollDTO> getAllPayslips() {
        return payslipRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public HrPayrollSummaryDTO getPayrollSummary() {
        List<Payslip> all = payslipRepository.findAll();

        BigDecimal totalNet = all.stream()
                .map(p -> p.getNetSalary() != null ? p.getNetSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBase = all.stream()
                .map(p -> p.getActualBaseSalary() != null ? p.getActualBaseSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDeductions = all.stream()
                .map(p -> p.getTotalDeduction() != null ? p.getTotalDeduction() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return HrPayrollSummaryDTO.builder()
                .totalNetSalary(totalNet)
                .totalBaseSalary(totalBase)
                .totalDeductions(totalDeductions)
                .employeeCount(all.size())
                .build();
    }

    private HrPayrollDTO mapToDTO(Payslip payslip) {
        String initials = "";
        String fullName = "Unknown";
        String departmentName = "N/A";
        String positionName = "N/A";

        if (payslip.getEmployee() != null) {
            if (payslip.getEmployee().getUser() != null) {
                fullName = payslip.getEmployee().getUser().getFullName() != null ? payslip.getEmployee().getUser().getFullName() : fullName;
                if (!"Unknown".equals(fullName) && !fullName.trim().isEmpty()) {
                    String[] names = fullName.trim().split("\\s+");
                    initials += names[0].charAt(0);
                    if (names.length > 1) {
                        initials += names[names.length - 1].charAt(0);
                    }
                }
            }
            if (payslip.getEmployee().getDepartment() != null) {
                departmentName = payslip.getEmployee().getDepartment().getName();
            }
            if (payslip.getEmployee().getPosition() != null) {
                positionName = payslip.getEmployee().getPosition().getName();
            }
        }

        return HrPayrollDTO.builder()
                .id(payslip.getId())
                .employeeName(fullName)
                .initials(initials.toUpperCase())
                .department(departmentName)
                .position(positionName)
                .basicSalary(payslip.getActualBaseSalary() != null ? payslip.getActualBaseSalary() : BigDecimal.ZERO)
                .totalAllowances(payslip.getTotalBonus() != null ? payslip.getTotalBonus() : BigDecimal.ZERO)
                .totalDeductions(payslip.getTotalDeduction() != null ? payslip.getTotalDeduction() : BigDecimal.ZERO)
                .netSalary(payslip.getNetSalary() != null ? payslip.getNetSalary() : BigDecimal.ZERO)
                .status(payslip.getStatus())
                .paymentDate(payslip.getPeriod() != null ? payslip.getPeriod().getEndDate() : null)
                .build();
    }
}
