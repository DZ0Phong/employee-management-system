package com.group5.ems.exception;

/**
 * Thrown when a workflow request cannot be found for the given ID.
 */
public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(Long id) {
        super("Workflow request with ID " + id + " not found");
    }

    public RequestNotFoundException(String message) {
        super(message);
    }
}
