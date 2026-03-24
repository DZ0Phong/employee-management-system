package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.PaginationDTO;
import com.group5.ems.dto.response.hrmanager.PayrollRunDTO;
import com.group5.ems.dto.response.hrmanager.PayrollSummaryDTO;
import com.group5.ems.entity.Payslip;
import com.group5.ems.repository.PayslipRepository;
import com.group5.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollApprovalService {

    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;

    // ── Summary ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PayrollSummaryDTO getSummary() {
        List<Payslip> pending = payslipRepository.findByStatus("PENDING");

        Map<Long, List<Payslip>> byDept = pending.stream()
                .filter(p -> p.getEmployee() != null && p.getEmployee().getDepartment() != null)
                .collect(Collectors.groupingBy(p -> p.getEmployee().getDepartment().getId()));

        BigDecimal totalValue = pending.stream()
                .map(Payslip::getNetSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int employeesCovered = (int) pending.stream()
                .map(Payslip::getEmployeeId)
                .distinct().count();

        return PayrollSummaryDTO.builder()
                .pendingCount(byDept.size())
                .pendingChangeLabel("pending departments")
                .pendingChangePositive(false)
                .totalValueFormatted(formatCurrency(totalValue))
                .employeesCovered(employeesCovered)
                .build();
    }

    // ── Payroll Runs ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PayrollRunDTO> getPayrollRuns(int page) {
        List<Payslip> payslips = payslipRepository.findByStatus("PENDING");

        Map<Long, List<Payslip>> byDept = payslips.stream()
                .filter(p -> p.getEmployee() != null && p.getEmployee().getDepartment() != null)
                .collect(Collectors.groupingBy(p -> p.getEmployee().getDepartment().getId()));

        return byDept.entrySet().stream()
                .map(entry -> {
                    List<Payslip> deptPayslips = entry.getValue();
                    Payslip first = deptPayslips.get(0);

                    String deptName   = first.getEmployee().getDepartment().getName();
                    String periodLabel = first.getPeriod() != null
                            ? first.getPeriod().getPeriodName()
                            : "Monthly Payroll";

                    BigDecimal total = deptPayslips.stream()
                            .map(Payslip::getNetSalary)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return PayrollRunDTO.builder()
                            .id(entry.getKey())
                            .departmentName(deptName)
                            .periodLabel(periodLabel)
                            .employeeCount(deptPayslips.size())
                            .totalAmountFormatted(formatCurrency(total))
                            .status(first.getStatus() != null ? first.getStatus() : "PENDING")
                            .dueDate(first.getPeriod() != null && first.getPeriod().getEndDate() != null 
                                    ? first.getPeriod().getEndDate() 
                                    : LocalDate.now().plusDays(5))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Approve by department ─────────────────────────────────────────────────
    @Transactional
    public void approveByDepartment(Long deptId, Long approverId) {
        payslipRepository.approveByDepartment(deptId, approverId);
    }

    // ── Pagination ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PaginationDTO getPagination(int page) {
        List<Payslip> pending = payslipRepository.findByStatus("PENDING");

        Map<Long, List<Payslip>> byDept = pending.stream()
                .filter(p -> p.getEmployee() != null && p.getEmployee().getDepartment() != null)
                .collect(Collectors.groupingBy(p -> p.getEmployee().getDepartment().getId()));

        int totalItems = byDept.size();
        int pageSize   = 10;
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        return PaginationDTO.builder()
                .currentPage(page)
                .totalPages(Math.max(totalPages, 1))
                .totalItems(totalItems)
                .startItem(totalItems == 0 ? 0 : (page - 1) * pageSize + 1)
                .endItem(Math.min(page * pageSize, totalItems))
                .build();
    }

    @Transactional
    public void rejectByDepartment(Long deptId, Long approverId, String note) {
        List<Payslip> payslips = payslipRepository.findByDepartmentAndStatus(deptId, "PENDING");
        payslips.forEach(p -> {
            p.setStatus("REJECTED");
            p.setApprovedBy(approverId);
        });
        payslipRepository.saveAll(payslips);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }
}