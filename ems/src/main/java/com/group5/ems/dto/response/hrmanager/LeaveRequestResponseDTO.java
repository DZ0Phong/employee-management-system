package com.group5.ems.dto.response.hrmanager;

import com.group5.ems.entity.Request;
import com.group5.ems.util.WorkingDayUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponseDTO {
    // Basic request info
    private Long id;
    private Long employeeId;
    private String title;              // Request title
    private String leaveType;
    private LocalDate leaveFrom;
    private LocalDate leaveTo;
    private String content;
    private String status;
    private String rejectedReason;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime approvedAt;

    // Priority info
    private String priority;           // CRITICAL, URGENT, HIGH, NORMAL
    private Integer priorityScore;     // 0-100

    // Employee info (for HR Manager view)
    private String employeeName;
    private String employeeInitials;
    private String employeePosition;
    private String department;  // Department name for filtering

    // Formatted fields (for template display)
    private String durationLabel;
    private String dateRange;
    private String dateRangeYear;
    private String reason;
    
    // Leave balance info
    private Integer currentBalance;
    private Integer usedThisYear;
    private Integer annualQuota;
    private Integer balanceAfterApproval;
    
    // Leave balance percentage (for progress bar)
    private Integer leaveBalanceTotal;
    private Integer leaveBalanceUsed;
    private Integer leaveBalanceRemaining;
    private Integer leaveBalancePercentage;  // 0-100
    
    // Team overlap info
    private Boolean hasOverlap;
    private Integer overlapCount;
    private String overlapEmployees;

    // Constructor from Request entity
    public LeaveRequestResponseDTO(Request request) {
        this.id = request.getId();
        this.title = request.getTitle();
        this.status = request.getStatus();
        this.leaveType = formatLeaveType(request.getLeaveType());
        this.leaveFrom = request.getLeaveFrom();
        this.leaveTo = request.getLeaveTo();
        this.content = request.getContent();
        this.rejectedReason = request.getRejectedReason();
        this.reason = request.getContent() != null ? request.getContent() : request.getTitle();
        this.createdAt = request.getCreatedAt();
        this.approvedAt = request.getApprovedAt();
        
        // Priority info
        this.priority = request.getPriority() != null ? request.getPriority() : "NORMAL";
        this.priorityScore = request.getPriorityScore() != null ? request.getPriorityScore() : 0;

        // Employee info
        if (request.getEmployee() != null && request.getEmployee().getUser() != null) {
            String fullName = request.getEmployee().getUser().getFullName();
            this.employeeName = fullName != null ? fullName : "Unknown Employee";
            this.employeeInitials = getInitials(fullName);
            this.employeeId = request.getEmployee().getId();
            
            if (request.getEmployee().getPosition() != null) {
                this.employeePosition = request.getEmployee().getPosition().getName();
            } else {
                this.employeePosition = "No Position";
            }
            
            if (request.getEmployee().getDepartment() != null) {
                this.department = request.getEmployee().getDepartment().getName();
            } else {
                this.department = "No Department";
            }
        } else {
            this.employeeName = "Unknown Employee";
            this.employeeInitials = "??";
            this.employeePosition = "No Position";
            this.department = "No Department";
        }

        // Date calculations
        if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
            this.dateRange = request.getLeaveFrom().format(formatter) + " - " + request.getLeaveTo().format(formatter);
            this.dateRangeYear = String.valueOf(request.getLeaveFrom().getYear());
            
            long days = WorkingDayUtils.countWorkingDays(request.getLeaveFrom(), request.getLeaveTo());
            this.durationLabel = days + (days == 1 ? " Day" : " Days");
        } else {
            this.dateRange = "No dates";
            this.dateRangeYear = "";
            this.durationLabel = "0 Days";
        }
        
        // Initialize overlap info (will be calculated by service if needed)
        this.hasOverlap = false;
        this.overlapCount = 0;
        this.overlapEmployees = null;
    }

    // Utility methods
    public long getDaysCount() {
        if (leaveFrom == null || leaveTo == null) return 0;
        return WorkingDayUtils.countWorkingDays(leaveFrom, leaveTo);
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "??";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
    }

    private String formatLeaveType(String leaveType) {
        if (leaveType == null || leaveType.isBlank()) {
            return "Leave";
        }
        String normalized = leaveType.trim().toUpperCase();
        return switch (normalized) {
            case "ANNUAL_LEAVE", "LEAVE_ANNUAL" -> "Annual Leave";
            case "SICK_LEAVE", "LEAVE_SICK" -> "Sick Leave";
            case "UNPAID_LEAVE", "PERSONAL_LEAVE", "LEAVE_UNPAID" -> "Unpaid Leave";
            case "MATERNITY_LEAVE", "LEAVE_MATERNITY" -> "Maternity Leave";
            case "PATERNITY_LEAVE", "LEAVE_PATERNITY" -> "Paternity Leave";
            case "BEREAVEMENT_LEAVE", "LEAVE_BEREAVEMENT" -> "Bereavement Leave";
            case "STUDY_LEAVE", "LEAVE_STUDY" -> "Study Leave";
            default -> leaveType.replace("_", " ");
        };
    }
}
