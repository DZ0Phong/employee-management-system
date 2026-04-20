package com.group5.ems.exception;

public class ReportOperationException extends RuntimeException {
    public ReportOperationException(String message) {
        super(message);
    }

    public ReportOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
