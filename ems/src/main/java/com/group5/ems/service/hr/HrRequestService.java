package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrRequestDTO;
import com.group5.ems.dto.response.HrRequestStatsDTO;
import com.group5.ems.constants.WorkflowConstants;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.RequestApprovalHistory;
import com.group5.ems.entity.RequestType;
import com.group5.ems.entity.User;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.exception.InvalidRejectionReasonException;
import com.group5.ems.exception.RequestNotFoundException;
import com.group5.ems.exception.WorkflowException;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.RequestApprovalHistoryRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.RequestTypeRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.common.ApprovalWorkflowService;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrRequestService {

    private final RequestRepository requestRepository;
    private final RequestApprovalHistoryRepository requestApprovalHistoryRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ApprovalWorkflowService workflowService;
    private final LogService logService;

    private static final int MIN_REJECTION_REASON_LENGTH = 10;
    private static final DateTimeFormatter DTF_FULL = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final List<String> LEAVE_CATEGORIES = List.of("ATTENDANCE");

    // ── Rejection reason categories ──
    private static final Map<String, String> REJECTION_CATEGORIES = new HashMap<>();
    static {
        REJECTION_CATEGORIES.put("NOT_JUSTIFIED", "Request Not Justified");
        REJECTION_CATEGORIES.put("BUDGET_CONSTRAINT", "Budget Constraint");
        REJECTION_CATEGORIES.put("POLICY_VIOLATION", "Policy Violation");
        REJECTION_CATEGORIES.put("DUPLICATE_REQUEST", "Duplicate Request");
        REJECTION_CATEGORIES.put("INCOMPLETE_INFO", "Incomplete Information");
        REJECTION_CATEGORIES.put("MANAGER_OVERRIDE", "Manager Override Decision");
        REJECTION_CATEGORIES.put("OTHER", "Other");
    }

    // ══════════════════════════════════════════════════════════════════
    // PENDING REQUESTS (Tab 1)
    // ══════════════════════════════════════════════════════════════════

    public List<HrRequestDTO> getPendingRequests() {
        return requestRepository.findPendingWorkflowRequests().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<HrRequestDTO> getHrmPendingRequests() {
        return requestRepository.findHrmPendingWorkflowRequests().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════
    // FILTERED HISTORY (Tab 2)
    // ══════════════════════════════════════════════════════════════════

    public Page<HrRequestDTO> getRequestHistoryFiltered(
            String status, String categoryCode, String search,
            LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {

        String safeStatus = (status != null && !status.isBlank()) ? status.trim() : null;
        String safeCategoryCode = (categoryCode != null && !categoryCode.isBlank()) ? categoryCode.trim() : null;
        String safeSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Request> page = requestRepository.findWorkflowRequestsFiltered(
                safeStatus, safeCategoryCode, safeSearch, dateFrom, dateTo, pageable);

        List<HrRequestDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // ══════════════════════════════════════════════════════════════════
    // APPROVE / REJECT with validation + audit logging
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public void approveRequest(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        if (!workflowService.canApprove(request, WorkflowConstants.ROLE_HR)) {
            throw new WorkflowException("Cannot approve request at current step. Current step: " + workflowService.getStepDisplayName(request.getStep()));
        }

        Long currentUserId = getCurrentUserId();
        workflowService.moveToNextStep(request, currentUserId, WorkflowConstants.ROLE_HR);
        
        logService.log(AuditAction.UPDATE, AuditEntityType.REQUEST, id);
    }

    @Transactional
    public void rejectRequest(Long id, String category, String reason) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        if (!workflowService.canApprove(request, WorkflowConstants.ROLE_HR)) {
            throw new WorkflowException("Cannot reject request at current step");
        }

        String fullReason = buildRejectionReason(category, reason);
        validateRejectionReason(fullReason);

        Long currentUserId = getCurrentUserId();
        workflowService.rejectRequest(request, currentUserId, WorkflowConstants.ROLE_HR, fullReason);
        
        logService.log(AuditAction.UPDATE, AuditEntityType.REQUEST, id);
    }

    // ══════════════════════════════════════════════════════════════════
    // BULK OPERATIONS
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public int bulkApprove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<Request> requests = requestRepository.findPendingWorkflowRequestsByIds(ids);
        Long currentUserId = getCurrentUserId();
        int count = 0;
        for (Request request : requests) {
            if (workflowService.canApprove(request, WorkflowConstants.ROLE_HR)) {
                workflowService.moveToNextStep(request, currentUserId, WorkflowConstants.ROLE_HR);
                logService.log(AuditAction.UPDATE, AuditEntityType.REQUEST, request.getId());
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int bulkReject(List<Long> ids, String category, String reason) {
        if (ids == null || ids.isEmpty()) return 0;
        String fullReason = buildRejectionReason(category, reason);
        validateRejectionReason(fullReason);

        List<Request> requests = requestRepository.findPendingWorkflowRequestsByIds(ids);
        Long currentUserId = getCurrentUserId();
        int count = 0;
        for (Request request : requests) {
            if (workflowService.canApprove(request, WorkflowConstants.ROLE_HR)) {
                workflowService.rejectRequest(request, currentUserId, WorkflowConstants.ROLE_HR, fullReason);
                logService.log(AuditAction.UPDATE, AuditEntityType.REQUEST, request.getId());
                count++;
            }
        }
        return count;
    }

    // ══════════════════════════════════════════════════════════════════
    // CREATE REQUEST (HR creates as themselves)
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public void createRequest(Long requestTypeId, String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Request title cannot be empty");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Request content cannot be empty");
        }

        RequestType requestType = requestTypeRepository.findById(requestTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid request type ID: " + requestTypeId));

        Long currentUserId = getCurrentUserId();
        Employee hrEmployee = employeeRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("No employee record found for current user"));

        Request request = new Request();
        request.setEmployeeId(hrEmployee.getId());
        request.setRequestTypeId(requestType.getId());
        request.setTitle(title.trim());
        request.setContent(content.trim());
        
        // Option B: Skip DM/HR -> DIRECT TO HRM
        request.setStatus(WorkflowConstants.STATUS_PENDING);
        request.setStep(WorkflowConstants.STEP_WAITING_HRM);
        
        Request saved = requestRepository.save(request);

        saveHistory(saved.getId(), currentUserId, "SUBMITTED", "Created by HR - Escalated to HR Manager");
        logService.log(AuditAction.CREATE, AuditEntityType.REQUEST, saved.getId());
    }

    // ══════════════════════════════════════════════════════════════════
    // STATISTICS (Tab 4)
    // ══════════════════════════════════════════════════════════════════

    public HrRequestStatsDTO getRequestStats() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();

        long totalPending = requestRepository.countPendingWorkflowRequests();
        long approvedThisMonth = requestRepository.countWorkflowByStatusSince("APPROVED", monthStart);
        long rejectedThisMonth = requestRepository.countWorkflowByStatusSince("REJECTED", monthStart);
        Double avgHours = requestRepository.avgWorkflowProcessingHoursSince(monthStart);

        List<Object[]> topTypes = requestRepository.findTopWorkflowTypes();
        String topRequestType = "N/A";
        long topRequestTypeCount = 0;
        if (topTypes != null && !topTypes.isEmpty()) {
            Object[] top = topTypes.get(0);
            topRequestType = top[0] != null ? top[0].toString() : "N/A";
            topRequestTypeCount = top[1] != null ? ((Number) top[1]).longValue() : 0;
        }

        return HrRequestStatsDTO.builder()
                .totalPending(totalPending)
                .approvedThisMonth(approvedThisMonth)
                .rejectedThisMonth(rejectedThisMonth)
                .avgProcessingHours(avgHours != null ? avgHours : 0.0)
                .topRequestType(topRequestType)
                .topRequestTypeCount(topRequestTypeCount)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // LOOKUP DATA
    // ══════════════════════════════════════════════════════════════════

    /**
     * Returns all non-leave request types that HR can create, sorted by category + name.
     */
    public List<RequestType> getCreatableRequestTypes() {
        return requestTypeRepository.findByCategoryNotInOrderByCategoryAscNameAsc(LEAVE_CATEGORIES);
    }

    public Map<String, String> getRejectionCategories() {
        return REJECTION_CATEGORIES;
    }

    // ══════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════

    private HrRequestDTO mapToDTO(Request request) {
        String empName = "Unknown";
        String initials = "?";
        String deptName = "N/A";
        Long deptId = null;
        String empCode = "N/A";
        String category = "N/A";
        String categoryCode = "N/A";

        if (request.getEmployee() != null) {
            Employee emp = request.getEmployee();
            if (emp.getUser() != null) {
                empName = emp.getUser().getFullName();
                initials = buildInitials(empName);
            }
            if (emp.getDepartment() != null) {
                deptName = emp.getDepartment().getName();
                deptId = emp.getDepartment().getId();
            }
            empCode = emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "N/A";
        }

        if (request.getRequestType() != null) {
            category = request.getRequestType().getName();
            categoryCode = request.getRequestType().getCategory();
        }

        String processedAt = null;
        if (request.getApprovedAt() != null) {
            processedAt = request.getApprovedAt().format(DTF_FULL);
        }

        String approverName = null;
        if (request.getApprovedByUser() != null) {
            approverName = request.getApprovedByUser().getFullName();
        }

        return HrRequestDTO.builder()
                .id(request.getId())
                .requestedBy(empName)
                .initials(initials)
                .department(deptName)
                .departmentId(deptId)
                .employeeCode(empCode)
                .category(category)
                .categoryCode(categoryCode)
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus())
                .rejectedReason(request.getRejectedReason())
                .submittedAt(request.getCreatedAt())
                .processedAt(processedAt)
                .approverName(approverName)
                .build();
    }

    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String buildRejectionReason(String category, String description) {
        String categoryLabel = REJECTION_CATEGORIES.getOrDefault(
                category != null ? category.trim().toUpperCase() : "OTHER",
                "Other"
        );
        String desc = (description != null && !description.isBlank()) ? description.trim() : "";
        return "[" + categoryLabel + "] " + desc;
    }

    private void validateRejectionReason(String fullReason) {
        if (fullReason == null || fullReason.length() < MIN_REJECTION_REASON_LENGTH) {
            throw new InvalidRejectionReasonException(
                    "Rejection reason must be at least " + MIN_REJECTION_REASON_LENGTH
                            + " characters (including category). Current length: "
                            + (fullReason != null ? fullReason.length() : 0));
        }
    }

    private void saveHistory(Long requestId, Long approverId, String action, String comment) {
        if (requestId == null || approverId == null) return;
        RequestApprovalHistory history = new RequestApprovalHistory();
        history.setRequestId(requestId);
        history.setApproverId(approverId);
        history.setAction(action);
        history.setComment(comment);
        requestApprovalHistoryRepository.save(history);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }
}