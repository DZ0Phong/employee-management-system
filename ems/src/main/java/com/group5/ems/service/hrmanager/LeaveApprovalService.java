package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.LeaveRequestResponseDTO;
import com.group5.ems.entity.Request;
import com.group5.ems.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
     * Approve a leave request
     */
    public boolean approveLeaveRequest(Long requestId, Long approverId) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            Request request = requestOpt.get();
            request.setStatus("APPROVED");
            request.setApprovedBy(approverId);
            request.setApprovedAt(LocalDateTime.now());
            request.setCurrentApproverId(null); // Clear current approver
            requestRepository.save(request);
            return true;
        }
        return false;
    }

    /**
     * Reject a leave request
     */
    public boolean rejectLeaveRequest(Long requestId, Long approverId, String rejectedReason) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            Request request = requestOpt.get();
            request.setStatus("REJECTED");
            request.setApprovedBy(approverId);
            request.setApprovedAt(LocalDateTime.now());
            request.setRejectedReason(rejectedReason);
            request.setCurrentApproverId(null); // Clear current approver
            requestRepository.save(request);
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
                .map(LeaveRequestResponseDTO::new)
                .collect(Collectors.toList());
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
}