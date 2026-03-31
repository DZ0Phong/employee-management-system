package com.group5.ems.service.hrmanager;

import com.group5.ems.constants.WorkflowConstants;
import com.group5.ems.dto.response.hrmanager.LeaveRequestResponseDTO;
import com.group5.ems.entity.EmployeeLeaveBalance;
import com.group5.ems.entity.Request;
import com.group5.ems.exception.WorkflowException;
import com.group5.ems.repository.EmployeeLeaveBalanceRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.service.common.ApprovalWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeaveApprovalService {

    @Autowired
    private RequestRepository requestRepository;
    
    @Autowired
    private EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;
    
    @Autowired
    private ApprovalWorkflowService workflowService;

    /**
     * Get all pending leave requests
     */
    public List<Request> getPendingLeaveRequests() {
        return requestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    /**
     * Get leave request by ID
     */
    public Optional<Request> getLeaveRequestById(Long id) {
        return requestRepository.findById(id);
    }

    /**
     * Approve a leave request and update balance
     */
    public boolean approveLeaveRequest(Long requestId, Long approverId) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            Request request = requestOpt.get();
            
            // Validate workflow step
            if (!workflowService.canApprove(request, WorkflowConstants.ROLE_HR_MANAGER)) {
                throw new WorkflowException("Cannot approve: Request must be approved by Department Manager and HR first. Current step: " 
                        + workflowService.getStepDisplayName(request.getStep()));
            }
            
            // Check for overlap before approving
            if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
                List<Request> overlappingRequests = requestRepository
                        .findOverlappingLeaveRequests(
                                "APPROVED",
                                request.getLeaveFrom(),
                                request.getLeaveTo()
                        );
                
                if (!overlappingRequests.isEmpty()) {
                    throw new RuntimeException("Cannot approve: " + overlappingRequests.size() + 
                            " team member(s) already on leave during this period");
                }
            }
            
            // Move to next step (will set to APPROVED since this is final step)
            workflowService.moveToNextStep(request, approverId, WorkflowConstants.ROLE_HR_MANAGER);
            
            // Update employee leave balance
            updateLeaveBalanceOnApproval(request);
            
            return true;
        }
        return false;
    }

    /**
     * Reject a leave request and update balance
     */
    public boolean rejectLeaveRequest(Long requestId, Long approverId, String rejectedReason) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            Request request = requestOpt.get();
            
            // Validate workflow step
            if (!workflowService.canApprove(request, WorkflowConstants.ROLE_HR_MANAGER)) {
                throw new WorkflowException("Cannot reject: Request must be at HR Manager approval step. Current step: " 
                        + workflowService.getStepDisplayName(request.getStep()));
            }
            
            // Reject using workflow service
            workflowService.rejectRequest(request, approverId, WorkflowConstants.ROLE_HR_MANAGER, rejectedReason);
            
            // Update employee leave balance (remove from pending)
            updateLeaveBalanceOnRejection(request);
            
            return true;
        }
        return false;
    }

    /**
     * Get approved leave requests
     */
    public List<Request> getApprovedLeaveRequests() {
        return requestRepository.findByStatusOrderByApprovedAtDesc("APPROVED");
    }

    /**
     * Get rejected leave requests
     */
    public List<Request> getRejectedLeaveRequests() {
        return requestRepository.findByStatusOrderByApprovedAtDesc("REJECTED");
    }

    /**
     * Get leave requests by employee ID
     */
    public List<Request> getLeaveRequestsByEmployeeId(Long employeeId) {
        return requestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    /**
     * Get leave requests by status
     */
    public List<Request> getLeaveRequestsByStatus(String status) {
        return requestRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Count pending leave requests
     */
    public long countPendingLeaveRequests() {
        return requestRepository.countByStatus("PENDING");
    }

    /**
     * Count approved leave requests this month
     */
    public long countApprovedLeaveRequestsThisMonth() {
        return requestRepository.countByStatusAndApprovedAtBetween(
            "APPROVED",
            LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0),
            LocalDateTime.now()
        );
    }

    /**
     * Get stats for leave approval dashboard
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount", countPendingLeaveRequests());
        stats.put("approvedThisMonth", countApprovedLeaveRequestsThisMonth());
        stats.put("totalRequests", requestRepository.count());
        
        // Get approved today count
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        long approvedToday = requestRepository.countByStatusAndApprovedAtBetween("APPROVED", todayStart, todayEnd);
        stats.put("approvedTodayCount", approvedToday);
        
        // Calculate completion rate (approved today / total requests today)
        long totalRequestsToday = requestRepository.countByCreatedAtBetween(todayStart, todayEnd);
        int completionRate = totalRequestsToday > 0 ? (int) ((approvedToday * 100) / totalRequestsToday) : 0;
        stats.put("completionRate", completionRate);
        
        // Get on leave now count (approved requests where current date is between leaveFrom and leaveTo)
        LocalDateTime now = LocalDateTime.now();
        long onLeaveNow = requestRepository.countByStatusAndLeaveFromLessThanEqualAndLeaveToGreaterThanEqual(
            "APPROVED", now.toLocalDate(), now.toLocalDate());
        stats.put("onLeaveNowCount", onLeaveNow);
        
        // Get next return info (earliest leaveTo date after today)
        List<Request> nextReturns = requestRepository.findByStatusAndLeaveToGreaterThanOrderByLeaveToAsc(
            "APPROVED", now.toLocalDate());
        if (!nextReturns.isEmpty()) {
            Request nextReturn = nextReturns.get(0);
            java.time.LocalDate returnDate = nextReturn.getLeaveTo();
            long daysUntilReturn = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), returnDate);
            if (daysUntilReturn == 0) {
                stats.put("nextReturnInfo", "Next return: Today");
            } else if (daysUntilReturn == 1) {
                stats.put("nextReturnInfo", "Next return: Tomorrow");
            } else {
                stats.put("nextReturnInfo", "Next return: " + daysUntilReturn + " days");
            }
        } else {
            stats.put("nextReturnInfo", "No upcoming returns");
        }
        
        // This month count and change
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long thisMonthCount = requestRepository.countByCreatedAtBetween(monthStart, now);
        stats.put("thisMonthCount", thisMonthCount);
        
        // Calculate change vs last month
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);
        LocalDateTime lastMonthEnd = monthStart.minusSeconds(1);
        long lastMonthCount = requestRepository.countByCreatedAtBetween(lastMonthStart, lastMonthEnd);
        
        if (lastMonthCount > 0) {
            double changePercent = ((thisMonthCount - lastMonthCount) * 100.0) / lastMonthCount;
            stats.put("thisMonthChange", String.format("%+.1f%% vs last month", changePercent));
        } else {
            stats.put("thisMonthChange", "vs last month");
        }
        
        return stats;
    }

    /**
     * Get leave requests with pagination
     */
    public List<LeaveRequestResponseDTO> getLeaveRequests(String tab, int page) {
        // Ensure page is at least 1
        if (page < 1) page = 1;
        
        Pageable pageable = PageRequest.of(page - 1, 10); // Spring uses 0-based indexing
        Page<Request> requestPage;
        
        switch (tab.toLowerCase()) {
            case "pending":
                requestPage = requestRepository.findRequestsByStatusWithoutLeaveTypeFilter("PENDING", pageable);
                break;
            case "approved":
                requestPage = requestRepository.findApprovedRequestsOrderByApprovedAt(pageable);
                break;
            case "rejected":
                requestPage = requestRepository.findRejectedRequestsOrderByApprovedAt(pageable);
                break;
            case "history":
                requestPage = requestRepository.findHistoryRequestsOrderByApprovedAt(pageable);
                break;
            default:
                requestPage = requestRepository.findAllRequestsWithoutLeaveTypeFilter(pageable);
                break;
        }
        
        return requestPage.getContent().stream()
                .map(request -> {
                    LeaveRequestResponseDTO dto = new LeaveRequestResponseDTO(request);
                    calculateLeaveBalance(dto, request);
                    calculateTeamOverlap(dto, request);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate leave balance for a request using employee_leave_balances table
     */
    private void calculateLeaveBalance(LeaveRequestResponseDTO dto, Request request) {
        if (request.getEmployee() == null) {
            dto.setCurrentBalance(0);
            dto.setUsedThisYear(0);
            dto.setAnnualQuota(20);
            dto.setBalanceAfterApproval(0);
            return;
        }
        
        Long employeeId = request.getEmployeeId();
        int currentYear = java.time.LocalDate.now().getYear();
        
        // Query from employee_leave_balances table
        Optional<EmployeeLeaveBalance> balanceOpt = employeeLeaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, currentYear);
        
        if (balanceOpt.isPresent()) {
            EmployeeLeaveBalance balance = balanceOpt.get();
            
            // Get values from database
            int totalDays = balance.getTotalDays().intValue();
            int usedDays = balance.getUsedDays().intValue();
            int pendingDays = balance.getPendingDays().intValue();
            int remainingDays = balance.getRemainingDays() != null 
                    ? balance.getRemainingDays().intValue() 
                    : (totalDays - usedDays - pendingDays);
            
            int requestDays = (int) dto.getDaysCount();
            int balanceAfter = remainingDays - requestDays;
            
            dto.setCurrentBalance(remainingDays);
            dto.setUsedThisYear(usedDays);
            dto.setAnnualQuota(totalDays);
            dto.setBalanceAfterApproval(Math.max(0, balanceAfter));
        } else {
            // Fallback if no balance record exists
            dto.setCurrentBalance(0);
            dto.setUsedThisYear(0);
            dto.setAnnualQuota(20);
            dto.setBalanceAfterApproval(0);
        }
    }

    /**
     * Get leave requests with pagination (alternative without leaveType filter)
     */
    public List<LeaveRequestResponseDTO> getAllRequests(String tab, int page) {
        // Ensure page is at least 1
        if (page < 1) page = 1;
        
        Pageable pageable = PageRequest.of(page - 1, 10);
        Page<Request> requestPage;
        
        switch (tab.toLowerCase()) {
            case "pending":
                requestPage = requestRepository.findRequestsByStatusWithoutLeaveTypeFilter("PENDING", pageable);
                break;
            case "approved":
                requestPage = requestRepository.findRequestsByStatusWithoutLeaveTypeFilter("APPROVED", pageable);
                break;
            case "rejected":
                requestPage = requestRepository.findRequestsByStatusWithoutLeaveTypeFilter("REJECTED", pageable);
                break;
            default:
                requestPage = requestRepository.findAllRequestsWithoutLeaveTypeFilter(pageable);
                break;
        }
        
        return requestPage.getContent().stream()
                .map(LeaveRequestResponseDTO::new)
                .collect(Collectors.toList());
    }
    public Map<String, Object> getPagination(String tab, int page) {
        // Ensure page is at least 1
        if (page < 1) page = 1;
        
        Pageable pageable = PageRequest.of(page - 1, 10); // Spring uses 0-based indexing
        Page<Request> requestPage;
        
        switch (tab.toLowerCase()) {
            case "pending":
                requestPage = requestRepository.findRequestsByStatusWithoutLeaveTypeFilter("PENDING", pageable);
                break;
            case "approved":
                requestPage = requestRepository.findApprovedRequestsOrderByApprovedAt(pageable);
                break;
            case "rejected":
                requestPage = requestRepository.findRejectedRequestsOrderByApprovedAt(pageable);
                break;
            case "history":
                requestPage = requestRepository.findHistoryRequestsOrderByApprovedAt(pageable);
                break;
            default:
                requestPage = requestRepository.findAllRequestsWithoutLeaveTypeFilter(pageable);
                break;
        }
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page); // 1-based for template
        pagination.put("totalPages", requestPage.getTotalPages());
        pagination.put("totalElements", requestPage.getTotalElements());
        pagination.put("hasNext", requestPage.hasNext());
        pagination.put("hasPrevious", requestPage.hasPrevious());
        
        // Calculate display range
        long totalElements = requestPage.getTotalElements();
        if (totalElements > 0) {
            pagination.put("startItem", (page - 1) * 10 + 1);
            pagination.put("endItem", Math.min(page * 10, (int) totalElements));
        } else {
            pagination.put("startItem", 0);
            pagination.put("endItem", 0);
        }
        pagination.put("totalItems", totalElements);
        
        return pagination;
    }
    
    /**
     * Update employee leave balance when request is approved
     */
    private void updateLeaveBalanceOnApproval(Request request) {
        if (request.getEmployee() == null || request.getLeaveFrom() == null || request.getLeaveTo() == null) {
            return;
        }
        
        Long employeeId = request.getEmployeeId();
        int currentYear = java.time.LocalDate.now().getYear();
        
        Optional<EmployeeLeaveBalance> balanceOpt = employeeLeaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, currentYear);
        
        if (balanceOpt.isPresent()) {
            EmployeeLeaveBalance balance = balanceOpt.get();
            
            // Calculate days
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    request.getLeaveFrom(), request.getLeaveTo()) + 1;
            
            // Update: pending -> used
            BigDecimal daysDecimal = BigDecimal.valueOf(days);
            balance.setPendingDays(balance.getPendingDays().subtract(daysDecimal));
            balance.setUsedDays(balance.getUsedDays().add(daysDecimal));
            
            // Recalculate remaining
            BigDecimal remaining = balance.getTotalDays()
                    .subtract(balance.getUsedDays())
                    .subtract(balance.getPendingDays());
            balance.setRemainingDays(remaining);
            balance.setUpdatedAt(java.time.Instant.now());
            
            employeeLeaveBalanceRepository.save(balance);
        }
    }
    
    /**
     * Update employee leave balance when request is rejected
     */
    private void updateLeaveBalanceOnRejection(Request request) {
        if (request.getEmployee() == null || request.getLeaveFrom() == null || request.getLeaveTo() == null) {
            return;
        }
        
        Long employeeId = request.getEmployeeId();
        int currentYear = java.time.LocalDate.now().getYear();
        
        Optional<EmployeeLeaveBalance> balanceOpt = employeeLeaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, currentYear);
        
        if (balanceOpt.isPresent()) {
            EmployeeLeaveBalance balance = balanceOpt.get();
            
            // Calculate days
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    request.getLeaveFrom(), request.getLeaveTo()) + 1;
            
            // Remove from pending
            BigDecimal daysDecimal = BigDecimal.valueOf(days);
            balance.setPendingDays(balance.getPendingDays().subtract(daysDecimal));
            
            // Recalculate remaining
            BigDecimal remaining = balance.getTotalDays()
                    .subtract(balance.getUsedDays())
                    .subtract(balance.getPendingDays());
            balance.setRemainingDays(remaining);
            balance.setUpdatedAt(java.time.Instant.now());
            
            employeeLeaveBalanceRepository.save(balance);
        }
    }

    /**
     * Calculate team overlap for a leave request
     */
    private void calculateTeamOverlap(LeaveRequestResponseDTO dto, Request request) {
        if (request.getLeaveFrom() == null || request.getLeaveTo() == null) {
            dto.setHasOverlap(false);
            dto.setOverlapCount(0);
            return;
        }

        try {
            // Find other approved/pending leave requests that overlap with this date range
            // Simplified: check all requests regardless of department
            List<Request> overlappingRequests = requestRepository
                    .findOverlappingLeaveRequests(
                            "APPROVED",
                            request.getLeaveFrom(),
                            request.getLeaveTo()
                    );

            // Exclude the current request itself
            overlappingRequests = overlappingRequests.stream()
                    .filter(r -> !r.getId().equals(request.getId()))
                    .collect(Collectors.toList());

            int overlapCount = overlappingRequests.size();
            dto.setHasOverlap(overlapCount > 0);
            dto.setOverlapCount(overlapCount);

            if (overlapCount > 0) {
                // Build overlap employees string - use safe access
                String overlapNames = overlappingRequests.stream()
                        .limit(3)
                        .map(r -> {
                            try {
                                if (r.getEmployee() != null && r.getEmployee().getUser() != null) {
                                    return r.getEmployee().getUser().getFullName();
                                }
                            } catch (Exception e) {
                                // Ignore lazy init errors
                            }
                            return "Unknown";
                        })
                        .collect(Collectors.joining(", "));

                if (overlapCount > 3) {
                    overlapNames += " and " + (overlapCount - 3) + " more";
                }

                dto.setOverlapEmployees(overlapNames + " on leave during this period");
            }
        } catch (Exception e) {
            // If any error, just set no overlap
            dto.setHasOverlap(false);
            dto.setOverlapCount(0);
        }
    }
}
