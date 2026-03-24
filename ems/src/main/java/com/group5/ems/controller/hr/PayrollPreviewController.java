package com.group5.ems.controller.hr;

import com.group5.ems.dto.response.hr.EmployeeAggregationDTO;
import com.group5.ems.dto.response.hr.PeriodSummaryDTO;
import com.group5.ems.exception.PayrollPreviewNotFoundException;
import com.group5.ems.service.admin.AdminService;
import com.group5.ems.service.hr.PayrollAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for the Payroll Preview dashboard.
 * Provides paginated aggregated view of attendance, bonuses, and deductions per period.
 */
@Controller
@RequestMapping("/hr/payroll-preview")
@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
@RequiredArgsConstructor
public class PayrollPreviewController {

    private static final int DEFAULT_PAGE_SIZE = 15;

    private final PayrollAggregationService aggregationService;
    private final AdminService adminService;

    /**
     * Displays the payroll preview dashboard for a given timesheet period.
     * Uses server-side pagination — only the current page of employees is loaded and aggregated.
     */
    @GetMapping("/{periodId}")
    public String previewPeriod(@PathVariable Long periodId,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        try {
            Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);

            // Server-side: only queries & aggregates the requested page of employees
            Page<EmployeeAggregationDTO> employeePage = aggregationService.getPaginatedPreview(periodId, pageable);
            PeriodSummaryDTO periodSummary = aggregationService.getPeriodSummary(periodId, employeePage);

            model.addAttribute("employees", employeePage);
            model.addAttribute("periodSummary", periodSummary);
            model.addAttribute("periodId", periodId);
            model.addAttribute("currentUser", adminService.getUserDTO().orElse(null));

            return "hr/payroll-preview";
        } catch (PayrollPreviewNotFoundException e) {
            throw new PayrollPreviewNotFoundException("Timesheet period not found with ID: " + periodId);
        }
    }

    /**
     * Generates payslips for all active employees in a locked period.
     * The period must be locked before payslips can be generated.
     */
    @PostMapping("/{periodId}/generate-payslips")
    public String generatePayslips(@PathVariable Long periodId,
                                    RedirectAttributes redirectAttributes) {
        try {
            int count = aggregationService.generatePayslips(periodId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully generated " + count + " payslip(s) for this period!");
        } catch (PayrollPreviewNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error: " + e.getMessage());
        }
        return "redirect:/hr/payroll-preview/" + periodId;
    }
}
