package com.group5.ems.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "requests")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── FK columns (raw) ──────────────────────────────────────────
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "request_type_id", nullable = false)
    private Long requestTypeId;

    @Column(name = "current_approver_id")
    private Long currentApproverId;

    @Column(name = "approved_by")
    private Long approvedBy;

    // ── Basic fields ──────────────────────────────────────────────
    @Size(max = 200)
    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Size(max = 255)
    @Column(name = "other_detail")
    private String otherDetail;

    // Leave-specific
    @Column(name = "leave_from")
    private LocalDate leaveFrom;

    @Column(name = "leave_to")
    private LocalDate leaveTo;

    @Size(max = 50)
    @Column(name = "leave_type", length = 50)
    private String leaveType;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    // ── Status / workflow ─────────────────────────────────────────
    @Column(length = 30)
    private String status = "PENDING";

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @ColumnDefault("'WAITING_DM'")
    @Size(max = 50)
    @Column(name = "step", length = 50)
    private String step;

    @ColumnDefault("0")
    @Column(name = "is_urgent")
    private boolean urgent;

    @Size(max = 20)
    @Column(name = "priority", length = 20)
    @ColumnDefault("'NORMAL'")
    private String priority = "NORMAL"; // CRITICAL, URGENT, HIGH, NORMAL

    @Column(name = "priority_score")
    @ColumnDefault("0")
    private Integer priorityScore = 0;

    // ── Timestamps ────────────────────────────────────────────────
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Relationships (read-only) ─────────────────────────────────
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dm_approver_id")
    private User dmApprover;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hrm_approver_id")
    private User hrmApprover;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hr_processor_id")
    private User hrProcessor;

    // ── Lifecycle ─────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}