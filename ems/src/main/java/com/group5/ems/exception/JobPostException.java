package com.group5.ems.exception;

public class JobPostException extends RuntimeException {

    public JobPostException(String message) {
        super(message);
    }

    public static JobPostException closeDateBeforeOpenDate() {
        return new JobPostException(
                "Close date cannot be before open date.");
    }

    public static JobPostException cannotOpenWithPastCloseDate(java.time.LocalDate closeDate) {
        return new JobPostException(
                "Cannot set status to OPEN: the close date (" + closeDate + ") is in the past.");
    }

    public static JobPostException hasActiveApplicants(long count) {
        return new JobPostException(
                "Cannot delete this job post: it has " + count +
                        " active applicant(s). Please close the posting instead.");
    }

    public static JobPostException notFound(Long id) {
        return new JobPostException(
                "Job post not found: " + id);
    }
}