package com.group5.ems.exception;

/**
 * Thrown when a payroll preview is requested for a timesheet period that does not exist.
 */
public class PayrollPreviewNotFoundException extends RuntimeException {

    public PayrollPreviewNotFoundException(Long periodId) {
        super("Timesheet period not found with ID: " + periodId);
    }

    public PayrollPreviewNotFoundException(String message) {
        super(message);
    }
}
