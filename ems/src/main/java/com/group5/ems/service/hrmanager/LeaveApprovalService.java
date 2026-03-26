package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.LeaveRequestResponseDTO;
import com.group5.ems.entity.EmployeeLeaveBalance;
import com.group5.ems.entity.Request;
import com.group5.ems.repository.EmployeeLeaveBalanceRepository;
import com.group5.ems.repository.RequestRepository;
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
            request.setStatus("APPROVED");
            request.setApprovedBy(approverId);
            request.setApprovedAt(LocalDateTime.now());
            request.setCurrentApproverId(null);
            requestRepository.save(request);
            
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
            request.setStatus("REJECTED");
            request.setApprovedBy(approverId);
            request.setApprovedAt(LocalDateTime.now());
            request.setRejectedReason(rejectedReason);
            request.setCurrentApproverId(null);
            requestRepository.save(request);
            
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
        
        // Additional stats for template
        stats.put("pendingChange", "+4 since yesterday"); // Mock data
        stats.put("approvedTodayCount", 8); // Mock data  
        stats.put("completionRate", 82); // Mock data
        stats.put("onLeaveNowCount", 12); // Mock data
        stats.put("nextReturnInfo", "Next return: Tomorrow"); // Mock data
        
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
                .map(request -> {
                    LeaveRequestResponseDTO dto = new LeaveRequestResponseDTO(request);
                    calculateLeaveBalance(dto, request);
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
                requestPage = requestRepository.findRequestsByStatusWithoutLeaveTypeFilter("APPROVED", pageable);
                break;
            case "rejected":
                requestPage = requestRepository.findRequestsByStatusWithoutLeaveTypeFilter("REJECTED", pageable);
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
}