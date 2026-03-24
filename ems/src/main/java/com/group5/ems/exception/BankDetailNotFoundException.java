package com.group5.ems.exception;

/**
 * Thrown when a bank detail record cannot be found for the given ID and employee.
 */
public class BankDetailNotFoundException extends RuntimeException {

    public BankDetailNotFoundException(Long bankId, Long employeeId) {
        super("Bank detail with ID " + bankId + " not found for employee ID " + employeeId);
    }

    public BankDetailNotFoundException(String message) {
        super(message);
    }
}
