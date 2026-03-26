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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PayrollReviewServiceImpl implements PayrollReviewService {

    private final PayslipRepository payslipRepository;
    private final TimesheetPeriodRepository periodRepository;

    @Value("${payroll.alerts.high-ot-amount:2000000}")
    private BigDecimal highOtThreshold;

    @Override
    public Page<PayslipReviewDTO> getPaginatedReview(Long periodId, String filter, Pageable pageable) {
        Page<PayslipReviewDTO> page;
        if ("negative".equalsIgnoreCase(filter)) {
            page = payslipRepository.findReviewDTONegativeNetByPeriodId(periodId, pageable);
        } else if ("overtime".equalsIgnoreCase(filter)) {
            page = payslipRepository.findReviewDTOHighOvertimeByPeriodId(periodId, highOtThreshold, pageable);
        } else if ("anomalies".equalsIgnoreCase(filter)) {
            page = payslipRepository.findReviewDTOAnomaliesByPeriodId(periodId, highOtThreshold, pageable);
        } else {
            page = payslipRepository.findReviewDTOByPeriodId(periodId, pageable);
        }
        
        return page.map(p -> new PayslipReviewDTO(
            p.payslipId(), p.employeeCode(), p.fullName(), p.actualBaseSalary(), 
            p.totalGross(), p.totalDeductions(), p.netSalary(), p.status(), 
            p.totalOtAmount(), p.totalBonus(), 
            p.netSalary() != null && p.netSalary().compareTo(BigDecimal.ZERO) <= 0,
            p.totalOtAmount() != null && p.totalOtAmount().compareTo(highOtThreshold) > 0
        ));
    }

    @Override
    public PayrollRunSummaryDTO getRunSummary(Long periodId) {
        TimesheetPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Period not found with ID: " + periodId));

        int totalPayslips = payslipRepository.countByPeriodId(periodId);
        BigDecimal totalGross = payslipRepository.sumTotalGrossByPeriodId(periodId);
        BigDecimal totalNet = payslipRepository.sumTotalNetByPeriodId(periodId);
        long pendingCount = payslipRepository.countPendingByPeriodId(periodId);

        int negativeCount = payslipRepository.countNegativeNetByPeriodId(periodId);
        int highOtCount = payslipRepository.countHighOvertimeByPeriodId(periodId, highOtThreshold);
        int anomalyCount = payslipRepository.countAnomaliesByPeriodId(periodId, highOtThreshold);

        String varianceIndicator = calculateVariance(period, totalNet);

        return new PayrollRunSummaryDTO(
                periodId,
                period.getPeriodName(),
                totalPayslips,
                totalGross != null ? totalGross : BigDecimal.ZERO,
                totalNet != null ? totalNet : BigDecimal.ZERO,
                pendingCount == 0 && totalPayslips > 0,
                varianceIndicator,
                anomalyCount,
                negativeCount,
                highOtCount
        );
    }

    private String calculateVariance(TimesheetPeriod currentPeriod, BigDecimal currentNet) {
        if (currentNet == null || currentNet.compareTo(BigDecimal.ZERO) == 0) return null;

        Optional<TimesheetPeriod> prevPeriodOpt = periodRepository.findPreviousLockedPeriod(currentPeriod.getStartDate());
        if (prevPeriodOpt.isEmpty()) return null;

        BigDecimal prevNet = payslipRepository.sumTotalNetByPeriodId(prevPeriodOpt.get().getId());
        if (prevNet == null || prevNet.compareTo(BigDecimal.ZERO) == 0) return null;

        BigDecimal diff = currentNet.subtract(prevNet);
        BigDecimal percentage = diff.divide(prevNet, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        String sign = percentage.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + percentage.setScale(1, RoundingMode.HALF_UP) + "% from last period";
    }

    @Override
    public byte[] exportDraftCsv(Long periodId) {
        List<PayslipReviewDTO> payslips = payslipRepository.findReviewDTOByPeriodId(periodId, Pageable.unpaged()).getContent();
        StringBuilder csv = new StringBuilder();
        csv.append("\"Employee Code\",\"Employee Name\",\"Actual Base Salary\",\"Total OT\",\"Total Bonus\",\"Total Deductions\",\"Net Salary\"\n");

        for (PayslipReviewDTO p : payslips) {
            String empName = p.fullName() != null ? p.fullName().replace("\"", "\"\"") : "";
            csv.append("\"").append(p.employeeCode()).append("\",")
               .append("\"").append(empName).append("\",")
               .append(p.actualBaseSalary() != null ? p.actualBaseSalary() : BigDecimal.ZERO).append(",")
               .append(p.totalOtAmount() != null ? p.totalOtAmount() : BigDecimal.ZERO).append(",")
               .append(p.totalBonus() != null ? p.totalBonus() : BigDecimal.ZERO).append(",")
               .append(p.totalDeductions() != null ? p.totalDeductions() : BigDecimal.ZERO).append(",")
               .append(p.netSalary() != null ? p.netSalary() : BigDecimal.ZERO).append("\n");
        }
        
        // Let's add the fix for UTF-8 BOM so Excel opens it correctly
        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF };
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);
        return result;
    }

}
