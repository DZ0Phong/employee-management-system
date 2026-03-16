package com.group5.ems.service.employee;

import com.group5.ems.dto.response.PayrollSummaryDTO;
import com.group5.ems.dto.response.PayslipDTO;

import java.util.List;

public interface PayrollService {
    PayrollSummaryDTO getPayrollSummary(Long employeeId);
    List<PayslipDTO> getPayslipHistory(Long employeeId);
    byte[] exportPayslipCsv(Long employeeId);
}