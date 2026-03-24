package com.group5.ems.service.hr.impl;

import com.group5.ems.dto.hr.PayrollRunSummaryDTO;
import com.group5.ems.dto.hr.PayslipReviewDTO;
import com.group5.ems.entity.TimesheetPeriod;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.PayslipRepository;
import com.group5.ems.repository.TimesheetPeriodRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.common.LogService;
import com.group5.ems.service.hr.PayrollReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PayrollReviewServiceImpl implements PayrollReviewService {

    private final PayslipRepository payslipRepository;
    private final TimesheetPeriodRepository periodRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    @Override
    public Page<PayslipReviewDTO> getPaginatedReview(Long periodId, Pageable pageable) {
        return payslipRepository.findReviewDTOByPeriodId(periodId, pageable);
    }

    @Override
    public PayrollRunSummaryDTO getRunSummary(Long periodId) {
        TimesheetPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Period not found with ID: " + periodId));

        int totalPayslips = payslipRepository.countByPeriodId(periodId);
        BigDecimal totalGross = payslipRepository.sumTotalGrossByPeriodId(periodId);
        BigDecimal totalNet = payslipRepository.sumTotalNetByPeriodId(periodId);
        long pendingCount = payslipRepository.countPendingByPeriodId(periodId);

        return new PayrollRunSummaryDTO(
                periodId,
                period.getPeriodName(),
                totalPayslips,
                totalGross != null ? totalGross : BigDecimal.ZERO,
                totalNet != null ? totalNet : BigDecimal.ZERO,
                pendingCount == 0 && totalPayslips > 0
        );
    }

    @Override
    @Transactional
    public void approvePayrollRun(Long periodId) {
        long pendingCount = payslipRepository.countPendingByPeriodId(periodId);
        if (pendingCount == 0) {
            throw new IllegalStateException("No pending payslips found to approve.");
        }

        Long currentUserId = resolveCurrentUserId();
        payslipRepository.approveAllPendingInPeriod(periodId, currentUserId);

        logService.log(AuditAction.UPDATE, AuditEntityType.PAYROLL, periodId);
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName())
                .map(user -> user.getId())
                .orElse(null);
    }
}
