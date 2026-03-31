package com.group5.ems.service.common;

import com.group5.ems.constants.WorkflowConstants;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.RequestApprovalHistory;
import com.group5.ems.exception.WorkflowException;
import com.group5.ems.repository.RequestApprovalHistoryRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalWorkflowService {
    
    private final RequestRepository requestRepository;
    private final RequestApprovalHistoryRepository historyRepository;
    private final UserRepository userRepository;
    
    /**
     * Check if user with given role can approve this request at current step
     */
    public boolean canApprove(Request request, String userRole) {
        if (request == null || userRole == null) {
            return false;
        }
        
        String currentStep = request.getStep();
        if (currentStep == null) {
            currentStep = WorkflowConstants.STEP_WAITING_DM;
        }
        
        // Check if request is still pending
        if (!WorkflowConstants.STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        
        // Match role with step
        switch (userRole) {
            case WorkflowConstants.ROLE_DEPT_MANAGER:
                return WorkflowConstants.STEP_WAITING_DM.equals(currentStep);
            case WorkflowConstants.ROLE_HR:
                return WorkflowConstants.STEP_WAITING_HR.equals(currentStep);
            case WorkflowConstants.ROLE_HR_MANAGER:
                return WorkflowConstants.STEP_WAITING_HRM.equals(currentStep);
            default:
                return false;
        }
    }
    
    /**
     * Move request to next approval step
     */
    @Transactional
    public void moveToNextStep(Request request, Long approverId, String approverRole) {
        if (request == null) {
            throw new WorkflowException("Request cannot be null");
        }
        
        if (!canApprove(request, approverRole)) {
            throw new WorkflowException("Cannot approve request at current step. Current step: " + request.getStep());
        }
        
        String currentStep = request.getStep();
        if (currentStep == null) {
            currentStep = WorkflowConstants.STEP_WAITING_DM;
        }
        
        // Move to next step
        switch (currentStep) {
            case WorkflowConstants.STEP_WAITING_DM:
                request.setStep(WorkflowConstants.STEP_WAITING_HR);
                request.setStatus(WorkflowConstants.STATUS_PENDING);
                saveHistory(request.getId(), approverId, "APPROVED_BY_DM", "Approved by Department Manager");
                break;
                
            case WorkflowConstants.STEP_WAITING_HR:
                request.setStep(WorkflowConstants.STEP_WAITING_HRM);
                request.setStatus(WorkflowConstants.STATUS_PENDING);
                saveHistory(request.getId(), approverId, "APPROVED_BY_HR", "Approved by HR");
                break;
                
            case WorkflowConstants.STEP_WAITING_HRM:
                request.setStep(WorkflowConstants.STEP_COMPLETED);
                request.setStatus(WorkflowConstants.STATUS_APPROVED);
                request.setApprovedAt(LocalDateTime.now());
                request.setApprovedBy(approverId);
                saveHistory(request.getId(), approverId, "APPROVED_BY_HRM", "Approved by HR Manager");
                break;
                
            default:
                throw new WorkflowException("Invalid workflow step: " + currentStep);
        }
        
        request.setCurrentApproverId(null);
        requestRepository.save(request);
    }
    
    /**
     * Reject request at any step
     */
    @Transactional
    public void rejectRequest(Request request, Long approverId, String approverRole, String reason) {
        if (request == null) {
            throw new WorkflowException("Request cannot be null");
        }
        
        if (!canApprove(request, approverRole)) {
            throw new WorkflowException("Cannot reject request at current step. Current step: " + request.getStep());
        }
        
        request.setStatus(WorkflowConstants.STATUS_REJECTED);
        request.setStep(WorkflowConstants.STEP_REJECTED);
        request.setRejectedReason(reason);
        request.setApprovedBy(approverId);
        request.setApprovedAt(LocalDateTime.now());
        request.setCurrentApproverId(null);
        
        String comment = "Rejected by " + getRoleDisplayName(approverRole);
        if (reason != null && !reason.isEmpty()) {
            comment += ": " + reason;
        }
        
        saveHistory(request.getId(), approverId, "REJECTED", comment);
        requestRepository.save(request);
    }
    
    /**
     * Get current workflow step display name
     */
    public String getStepDisplayName(String step) {
        if (step == null) {
            return "Waiting for Department Manager";
        }
        
        switch (step) {
            case WorkflowConstants.STEP_WAITING_DM:
                return "Waiting for Department Manager";
            case WorkflowConstants.STEP_WAITING_HR:
                return "Waiting for HR";
            case WorkflowConstants.STEP_WAITING_HRM:
                return "Waiting for HR Manager";
            case WorkflowConstants.STEP_COMPLETED:
                return "Completed";
            case WorkflowConstants.STEP_REJECTED:
                return "Rejected";
            default:
                return step;
        }
    }
    
    private String getRoleDisplayName(String role) {
        switch (role) {
            case WorkflowConstants.ROLE_DEPT_MANAGER:
                return "Department Manager";
            case WorkflowConstants.ROLE_HR:
                return "HR";
            case WorkflowConstants.ROLE_HR_MANAGER:
                return "HR Manager";
            default:
                return role;
        }
    }
    
    private void saveHistory(Long requestId, Long approverId, String action, String comment) {
        RequestApprovalHistory history = new RequestApprovalHistory();
        history.setRequestId(requestId);
        history.setApproverId(approverId);
        history.setAction(action);
        history.setComment(comment);
        historyRepository.save(history);
    }
}
