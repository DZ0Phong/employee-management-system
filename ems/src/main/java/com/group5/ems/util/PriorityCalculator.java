package com.group5.ems.util;

import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Advanced Priority Calculator for Requests
 * Calculates priority score based on multiple factors:
 * - Waiting time (40 points max)
 * - Request type (30 points max)
 * - Deadline/start date (20 points max)
 * - Employee level (5 points max)
 * - Team impact (5 points max)
 */
public class PriorityCalculator {

    /**
     * Calculate priority score and level for a request
     * @return array [score, level] where level is CRITICAL/URGENT/HIGH/NORMAL
     */
    public static Object[] calculatePriority(Request request, Employee employee) {
        int score = 0;

        // Factor 1: Waiting time (40 points max)
        score += calculateWaitingTimeScore(request);

        // Factor 2: Request type (30 points max)
        score += calculateRequestTypeScore(request);

        // Factor 3: Deadline/start date (20 points max)
        score += calculateDeadlineScore(request);

        // Factor 4: Employee level (5 points max)
        score += calculateEmployeeLevelScore(employee);

        // Factor 5: Team impact (5 points max) - simplified for now
        score += calculateTeamImpactScore(request);

        // Determine priority level
        String level;
        if (score >= 60) {
            level = "CRITICAL";
        } else if (score >= 40) {
            level = "URGENT";
        } else if (score >= 20) {
            level = "HIGH";
        } else {
            level = "NORMAL";
        }

        return new Object[]{score, level};
    }

    /**
     * Factor 1: Waiting time score (40 points max)
     * Longer waiting = higher priority
     */
    private static int calculateWaitingTimeScore(Request request) {
        if (request.getCreatedAt() == null) return 0;

        long hoursWaiting = ChronoUnit.HOURS.between(request.getCreatedAt(), LocalDateTime.now());

        if (hoursWaiting > 72) return 40;      // > 3 days
        if (hoursWaiting > 48) return 25;      // > 2 days
        if (hoursWaiting > 24) return 15;      // > 1 day
        if (hoursWaiting > 12) return 8;       // > 12 hours
        return 0;
    }

    /**
     * Factor 2: Request type score (30 points max)
     * Emergency/Sick = highest priority
     */
    private static int calculateRequestTypeScore(Request request) {
        String leaveType = request.getLeaveType();
        if (leaveType == null) return 5; // Default for non-leave requests

        switch (leaveType.toUpperCase()) {
            case "SICK":
            case "EMERGENCY":
                return 30;
            case "MATERNITY":
            case "PATERNITY":
                return 20;
            case "BEREAVEMENT":
                return 25;
            case "UNPAID":
                return 15;
            case "ANNUAL":
            default:
                return 10;
        }
    }

    /**
     * Factor 3: Deadline/start date score (20 points max)
     * Sooner start date = higher priority
     */
    private static int calculateDeadlineScore(Request request) {
        if (request.getLeaveFrom() == null) return 0;

        long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(), request.getLeaveFrom());

        if (daysUntilStart < 0) return 20;     // Already started (overdue!)
        if (daysUntilStart <= 2) return 20;    // Starts in 2 days
        if (daysUntilStart <= 5) return 15;    // Starts in 5 days
        if (daysUntilStart <= 7) return 10;    // Starts in a week
        if (daysUntilStart <= 14) return 5;    // Starts in 2 weeks
        return 0;
    }

    /**
     * Factor 4: Employee level score (5 points max)
     * Higher position = slightly higher priority
     */
    private static int calculateEmployeeLevelScore(Employee employee) {
        if (employee == null || employee.getPosition() == null) return 0;

        // Assuming position has a level field or we can infer from name
        String positionName = employee.getPosition().getName();
        if (positionName == null) return 0;

        String posLower = positionName.toLowerCase();
        if (posLower.contains("director") || posLower.contains("vp") || posLower.contains("ceo")) {
            return 5;
        }
        if (posLower.contains("manager") || posLower.contains("lead")) {
            return 3;
        }
        if (posLower.contains("senior")) {
            return 2;
        }
        return 1; // Regular employee
    }

    /**
     * Factor 5: Team impact score (5 points max)
     * Simplified: based on leave duration
     */
    private static int calculateTeamImpactScore(Request request) {
        if (request.getLeaveFrom() == null || request.getLeaveTo() == null) return 0;

        long leaveDays = ChronoUnit.DAYS.between(request.getLeaveFrom(), request.getLeaveTo()) + 1;

        if (leaveDays >= 15) return 5;  // Long leave = high impact
        if (leaveDays >= 10) return 4;
        if (leaveDays >= 5) return 3;
        if (leaveDays >= 3) return 2;
        return 1;
    }

    /**
     * Get priority badge color for UI
     */
    public static String getPriorityColor(String priority) {
        if (priority == null) return "slate";
        switch (priority.toUpperCase()) {
            case "CRITICAL": return "red";
            case "URGENT": return "orange";
            case "HIGH": return "yellow";
            case "NORMAL":
            default: return "slate";
        }
    }

    /**
     * Get priority icon for UI
     */
    public static String getPriorityIcon(String priority) {
        if (priority == null) return "flag";
        switch (priority.toUpperCase()) {
            case "CRITICAL": return "error";
            case "URGENT": return "warning";
            case "HIGH": return "flag";
            case "NORMAL":
            default: return "outlined_flag";
        }
    }
}
