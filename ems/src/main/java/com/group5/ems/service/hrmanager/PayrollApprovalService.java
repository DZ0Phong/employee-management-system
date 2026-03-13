package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.PayrollRunDTO;
import com.group5.ems.dto.response.hrmanager.PayrollSummaryDTO;
import com.group5.ems.dto.response.hrmanager.PaginationDTO;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollApprovalService {

    private final PayslipRepository payslipRepository; // Sửa từ SalaryRepository
    private final EmployeeRepository employeeRepository;
    private static final int PAGE_SIZE = 10;

    // ── Summary cards ─────────────────────────────────────────────────────────
    public PayrollSummaryDTO getSummary() {
        long pendingCount    = payslipRepository.countByStatus("PENDING"); // Sửa thành long
        long totalEmployees  = employeeRepository.count();

        // TODO: tính totalValue từ DB sau
        return PayrollSummaryDTO.builder()
                .pendingCount((int) pendingCount) // Cast về int khi cần
                .pendingChangeLabel("↑ 2 from last month")
                .pendingChangePositive(false)
                .totalValueFormatted("€450,230")
                .employeesCovered((int) totalEmployees)
                .build();
    }

    // ── Danh sách payroll runs ────────────────────────────────────────────────
    public List<PayrollRunDTO> getPayrollRuns(int page) {
        // TODO: thay bằng query thực sau khi có bảng PayrollRun
        return Arrays.asList(
                PayrollRunDTO.builder()
                        .id(1L).departmentName("Engineering (UK)")
                        .periodLabel("Monthly Payroll - June 2024")
                        .employeeCount(342).totalAmountFormatted("€128,450.00")
                        .status("PENDING_REVIEW")
                        .dueDate(java.time.LocalDate.of(2024, 6, 25))
                        .build(),
                PayrollRunDTO.builder()
                        .id(2L).departmentName("Sales & Marketing")
                        .periodLabel("Monthly Payroll - June 2024")
                        .employeeCount(156).totalAmountFormatted("€85,200.00")
                        .status("PENDING_REVIEW")
                        .dueDate(java.time.LocalDate.of(2024, 6, 25))
                        .build(),
                PayrollRunDTO.builder()
                        .id(3L).departmentName("Customer Success")
                        .periodLabel("Monthly Payroll - June 2024")
                        .employeeCount(89).totalAmountFormatted("€42,150.00")
                        .status("PROCESSING")
                        .dueDate(java.time.LocalDate.of(2024, 6, 26))
                        .build(),
                PayrollRunDTO.builder()
                        .id(4L).departmentName("Operations (Germany)")
                        .periodLabel("Monthly Payroll - June 2024")
                        .employeeCount(210).totalAmountFormatted("€112,000.00")
                        .status("APPROVED")
                        .dueDate(java.time.LocalDate.of(2024, 6, 24))
                        .build(),
                PayrollRunDTO.builder()
                        .id(5L).departmentName("Executive & Admin")
                        .periodLabel("Monthly Payroll - June 2024")
                        .employeeCount(45).totalAmountFormatted("€82,430.00")
                        .status("PENDING_REVIEW")
                        .dueDate(java.time.LocalDate.of(2024, 6, 25))
                        .build()
        );
    }

    // ── Pagination ────────────────────────────────────────────────────────────
    public PaginationDTO getPagination(int page) {
        // TODO: thay bằng query thực sau
        return PaginationDTO.builder()
                .currentPage(page)
                .totalPages(2)
                .totalItems(12)
                .startItem((page - 1) * PAGE_SIZE + 1)
                .endItem(Math.min(page * PAGE_SIZE, 12))
                .build();
    }
}