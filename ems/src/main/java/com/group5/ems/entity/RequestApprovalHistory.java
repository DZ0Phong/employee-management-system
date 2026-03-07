package com.group5.ems.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_approval_history")
public class RequestApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    @Column(nullable = false, length = 30)
    private String action; // APPROVED, REJECTED, FORWARDED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "action_at", updatable = false)
    private LocalDateTime actionAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", insertable = false, updatable = false)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", insertable = false, updatable = false)
    private User approver;

    @PrePersist
    protected void onCreate() {
        actionAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getActionAt() { return actionAt; }
    public void setActionAt(LocalDateTime actionAt) { this.actionAt = actionAt; }
    public Request getRequest() { return request; }
    public void setRequest(Request request) { this.request = request; }
    public User getApprover() { return approver; }
    public void setApprover(User approver) { this.approver = approver; }
}
