package com.group5.ems.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "staffing_requests")
public class StaffingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "requested_by_employee_id", nullable = false)
    private Long requestedByEmployeeId;

    @Size(max = 50)
    @Column(name = "request_type", length = 50, nullable = false)
    private String requestType; // RECRUITMENT, TRANSFER

    @Size(max = 200)
    @Column(name = "role_requested", length = 200, nullable = false)
    private String roleRequested;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 30)
    @Column(name = "status", length = 30, nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, COMPLETED

    @Column(name = "assigned_employee_id")
    private Long assignedEmployeeId;

    @Column(name = "processed_by_user_id")
    private Long processedByUserId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_employee_id", insertable = false, updatable = false)
    private Employee requestedByEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id", insertable = false, updatable = false)
    private Employee assignedEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id", insertable = false, updatable = false)
    private User processedByUser;

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
