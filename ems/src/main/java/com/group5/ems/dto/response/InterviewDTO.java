package com.group5.ems.dto.response;

/**
 * DTO trả về thông tin một buổi phỏng vấn.
 * Dùng cho cả "My Interviews" (interviewer xem lịch của mình)
 * và candidate detail modal (HR xem interview của 1 application).
 */
public class InterviewDTO {

    private Long id;
    private Long applicationId;

    // Candidate info
    private String candidateName;
    private String candidateInitials;
    private String candidateEmail;

    // Job info
    private String jobTitle;
    private String department;

    // Interview info
    private String scheduledAt;       // formatted string, e.g. "May 12, 2025 14:00"
    private String scheduledAtRaw;    // ISO string for <input type="datetime-local">
    private String location;
    private String status;            // SCHEDULED | COMPLETED | CANCELLED
    private String feedback;

    // Assigned by
    private String assignedByName;

    public InterviewDTO() {}

    public InterviewDTO(Long id, Long applicationId,
                        String candidateName, String candidateInitials, String candidateEmail,
                        String jobTitle, String department,
                        String scheduledAt, String scheduledAtRaw,
                        String location, String status, String feedback,
                        String assignedByName) {
        this.id = id;
        this.applicationId = applicationId;
        this.candidateName = candidateName;
        this.candidateInitials = candidateInitials;
        this.candidateEmail = candidateEmail;
        this.jobTitle = jobTitle;
        this.department = department;
        this.scheduledAt = scheduledAt;
        this.scheduledAtRaw = scheduledAtRaw;
        this.location = location;
        this.status = status;
        this.feedback = feedback;
        this.assignedByName = assignedByName;
    }

    // ── getters / setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateInitials() { return candidateInitials; }
    public void setCandidateInitials(String candidateInitials) { this.candidateInitials = candidateInitials; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getScheduledAtRaw() { return scheduledAtRaw; }
    public void setScheduledAtRaw(String scheduledAtRaw) { this.scheduledAtRaw = scheduledAtRaw; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }
}