package com.group5.ems.exception;

/**
 * Thrown when trying to approve or reject a workflow request
 * that has already been processed (not in PENDING status).
 */
public class RequestAlreadyProcessedException extends RuntimeException {

    public RequestAlreadyProcessedException(Long id, String currentStatus) {
        super("Request with ID " + id + " has already been processed. Current status: " + currentStatus);
    }
}
