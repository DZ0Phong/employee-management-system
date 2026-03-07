package com.group5.ems.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "request_type_id", nullable = false)
    private Long requestTypeId;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "leave_from")
    private LocalDate leaveFrom;

    @Column(name = "leave_to")
    private LocalDate leaveTo;

    @Column(name = "leave_type", length = 50)
    private String leaveType; // ANNUAL_LEAVE, SICK_LEAVE, UNPAID_LEAVE, ...

    @Column(length = 30)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "current_approver_id")
    private Long currentApproverId;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_type_id", insertable = false, updatable = false)
    private RequestType requestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approver_id", insertable = false, updatable = false)
    private User currentApprover;

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
    public Long getRequestTypeId() { return requestTypeId; }
    public void setRequestTypeId(Long requestTypeId) { this.requestTypeId = requestTypeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getLeaveFrom() { return leaveFrom; }
    public void setLeaveFrom(LocalDate leaveFrom) { this.leaveFrom = leaveFrom; }
    public LocalDate getLeaveTo() { return leaveTo; }
    public void setLeaveTo(LocalDate leaveTo) { this.leaveTo = leaveTo; }
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }
    public Long getCurrentApproverId() { return currentApproverId; }
    public void setCurrentApproverId(Long currentApproverId) { this.currentApproverId = currentApproverId; }
    public User getCurrentApprover() { return currentApprover; }
    public void setCurrentApprover(User currentApprover) { this.currentApprover = currentApprover; }
    public User getApprovedByUser() { return approvedByUser; }
    public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
}

