package com.group5.ems.exception;

/**
 * Thrown when attempting to approve/reject a leave request that has already been processed.
 */
public class LeaveRequestAlreadyProcessedException extends RuntimeException {

    public LeaveRequestAlreadyProcessedException(Long id, String currentStatus) {
        super("Leave request with ID " + id + " has already been processed (status: " + currentStatus + ")");
    }
}
