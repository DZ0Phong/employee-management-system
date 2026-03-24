package com.group5.ems.exception;

/**
 * Thrown when attempting to lock or modify a period that is already locked.
 */
public class PeriodAlreadyLockedException extends RuntimeException {

    public PeriodAlreadyLockedException() {
        super("This period is already locked and cannot be modified.");
    }

    public PeriodAlreadyLockedException(String message) {
        super(message);
    }
}
