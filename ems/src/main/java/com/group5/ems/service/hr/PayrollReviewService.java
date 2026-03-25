package com.group5.ems.service.hr;

import com.group5.ems.dto.hr.PayrollRunSummaryDTO;
import com.group5.ems.dto.hr.PayslipReviewDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PayrollReviewService {
    Page<PayslipReviewDTO> getPaginatedReview(Long periodId, Pageable pageable);
    PayrollRunSummaryDTO getRunSummary(Long periodId);
}
