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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/hr/payroll-review")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'HR_MANAGER', 'ADMIN')")
public class PayrollReviewController {

    private final PayrollReviewService payrollReviewService;

    @GetMapping("/{periodId}")
    public String reviewDashboard(
            @PathVariable Long periodId,
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {
        
        PayrollRunSummaryDTO summary = payrollReviewService.getRunSummary(periodId);
        Page<PayslipReviewDTO> payslips = payrollReviewService.getPaginatedReview(periodId, filter, pageable);
        
        model.addAttribute("summary", summary);
        model.addAttribute("payslips", payslips);
        model.addAttribute("periodId", periodId);
        model.addAttribute("currentFilter", filter);
        
        return "hr/payroll-review";
    }

    @GetMapping("/{periodId}/export-draft")
    public ResponseEntity<byte[]> exportDraftCsv(@PathVariable Long periodId) {
        byte[] csvData = payrollReviewService.exportDraftCsv(periodId);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=draft-payroll-" + periodId + ".csv");
        headers.set(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }

}
