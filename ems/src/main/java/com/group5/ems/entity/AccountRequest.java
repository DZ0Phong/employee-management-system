package com.group5.ems.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_requests")
public class AccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "proposed_role_id", nullable = false)
    private Long proposedRoleId;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "request_reason", columnDefinition = "TEXT")
    private String requestReason;

    @Column(length = 30)
    private String status = "PENDING";

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_role_id", insertable = false, updatable = false)
    private Role proposedRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", insertable = false, updatable = false)
    private User requestedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", insertable = false, updatable = false)
    private User approvedByUser;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Long getProposedRoleId() { return proposedRoleId; }
    public void setProposedRoleId(Long proposedRoleId) { this.proposedRoleId = proposedRoleId; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public String getRequestReason() { return requestReason; }
    public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Role getProposedRole() { return proposedRole; }
    public void setProposedRole(Role proposedRole) { this.proposedRole = proposedRole; }
    public User getRequestedByUser() { return requestedByUser; }
    public void setRequestedByUser(User requestedByUser) { this.requestedByUser = requestedByUser; }
    public User getApprovedByUser() { return approvedByUser; }
    public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
}
