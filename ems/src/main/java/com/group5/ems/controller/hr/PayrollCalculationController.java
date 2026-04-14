package com.group5.ems.controller.hr;

import com.group5.ems.exception.PayrollPreviewNotFoundException;
import com.group5.ems.service.hr.PayrollCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
@Controller
@RequestMapping("/hr/payroll-periods")
@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
@RequiredArgsConstructor
public class PayrollCalculationController {

    private final PayrollCalculationService calculationService;

    @PostMapping("/{periodId}/generate")
    public String generatePayslips(@PathVariable Long periodId, RedirectAttributes redirectAttributes) {
        try {
            int count = calculationService.generateDraftPayslips(periodId);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Gross-to-Net calculation completed! Successfully generated " + count + " draft payslip(s).");
            
            // Redirect to review dashboard (assume Task 4.1 route exists)
            return "redirect:/hr/payroll-review/" + periodId;
            
        } catch (PayrollPreviewNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Timesheet period not found.");
            return "redirect:/hr/payroll-preview/" + periodId;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/hr/payroll-preview/" + periodId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during calculation.");
            return "redirect:/hr/payroll-preview/" + periodId;
        }
    }
}
*/
