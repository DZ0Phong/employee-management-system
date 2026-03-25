package com.group5.ems.controller.hr;

import com.group5.ems.dto.hr.PayrollRunSummaryDTO;
import com.group5.ems.dto.hr.PayslipReviewDTO;
import com.group5.ems.service.hr.PayrollReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hr/payroll-review")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'HR_MANAGER', 'ADMIN')")
public class PayrollReviewController {

    private final PayrollReviewService payrollReviewService;

    @GetMapping("/{periodId}")
    public String reviewDashboard(
            @PathVariable Long periodId,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {
        
        PayrollRunSummaryDTO summary = payrollReviewService.getRunSummary(periodId);
        Page<PayslipReviewDTO> payslips = payrollReviewService.getPaginatedReview(periodId, pageable);
        
        model.addAttribute("summary", summary);
        model.addAttribute("payslips", payslips);
        model.addAttribute("periodId", periodId);
        
        return "hr/payroll-review";
    }

}
