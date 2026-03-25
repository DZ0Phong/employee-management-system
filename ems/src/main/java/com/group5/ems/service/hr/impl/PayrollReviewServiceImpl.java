package com.group5.ems.service.hr.impl;

import com.group5.ems.dto.hr.PayrollRunSummaryDTO;
import com.group5.ems.dto.hr.PayslipReviewDTO;
import com.group5.ems.entity.TimesheetPeriod;
import com.group5.ems.repository.PayslipRepository;
import com.group5.ems.repository.TimesheetPeriodRepository;
import com.group5.ems.service.hr.PayrollReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PayrollReviewServiceImpl implements PayrollReviewService {

    private final PayslipRepository payslipRepository;
    private final TimesheetPeriodRepository periodRepository;

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

}
