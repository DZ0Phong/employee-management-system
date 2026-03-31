package com.group5.ems.exception;

/**
 * Thrown when a rejection reason is blank or too short (minimum 10 characters required).
 */
public class InvalidRejectionReasonException extends RuntimeException {

    public InvalidRejectionReasonException() {
        super("Rejection reason is required and must be at least 10 characters long");
    }

    public InvalidRejectionReasonException(String message) {
        super(message);
    }
}
