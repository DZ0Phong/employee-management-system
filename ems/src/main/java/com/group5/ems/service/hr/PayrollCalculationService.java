package com.group5.ems.service.hr;

/**
 * Service for calculating payroll and generating payslips.
 */
public interface PayrollCalculationService {
    
    /**
     * Generates draft payslips for a given timesheet period.
     * Calculates gross-to-net salary including base, OT, and bonuses.
     *
     * @param periodId the timesheet period ID
     * @return the number of payslips generated
     * @throws IllegalStateException if the period is not locked or payslips already exist
     */
    int generateDraftPayslips(Long periodId);
}
