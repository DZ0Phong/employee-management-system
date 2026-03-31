package com.group5.ems.exception;

/**
 * Thrown when a leave request cannot be found for the given ID.
 */
public class LeaveRequestNotFoundException extends RuntimeException {

    public LeaveRequestNotFoundException(Long id) {
        super("Leave request with ID " + id + " not found");
    }

    public LeaveRequestNotFoundException(String message) {
        super(message);
    }
}
