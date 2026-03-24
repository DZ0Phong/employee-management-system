package com.group5.ems.exception;

/**
 * Thrown when a new timesheet period overlaps with an existing one.
 */
public class PeriodOverlapException extends RuntimeException {

    public PeriodOverlapException() {
        super("This timesheet period overlaps with an existing one.");
    }

    public PeriodOverlapException(String message) {
        super(message);
    }
}
