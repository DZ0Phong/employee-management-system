package com.group5.ems.controller.hr;

import com.group5.ems.service.hr.BankExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BankExportController {

    private final BankExportService bankExportService;

    @GetMapping("/hr/payroll-periods/{periodId}/export-bank")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('ADMIN')")
    public void exportBankFile(
            @PathVariable Long periodId,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) throws IOException {
        
        try {
            String csvContent = bankExportService.generateCsvContent(periodId);
            
            // Set Headers for file download
            String filename = String.format("Bank_Export_Period_%d.csv", periodId);
            response.setContentType("text/csv");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            
            // Write content to response
            response.getWriter().write(csvContent);
            response.getWriter().flush();
            
        } catch (IllegalStateException e) {
            log.warn("Bank export failed for period {}: {}", periodId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            response.sendRedirect("/hr/payroll-review/" + periodId);
        } catch (Exception e) {
            log.error("Unexpected error during bank export for period {}: ", periodId, e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during bank export.");
            response.sendRedirect("/hr/payroll-review/" + periodId);
        }
    }
}
