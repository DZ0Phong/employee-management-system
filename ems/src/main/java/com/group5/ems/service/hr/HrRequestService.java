package com.group5.ems.service.hr;

import com.group5.ems.dto.request.RecruitmentTicketDTO;
import com.group5.ems.dto.response.HrRequestDTO;

import com.group5.ems.constants.WorkflowConstants;
import com.group5.ems.entity.BenefitType;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Position;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.RequestApprovalHistory;
import com.group5.ems.entity.RequestType;
import com.group5.ems.entity.User;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.exception.InvalidRejectionReasonException;
import com.group5.ems.exception.RequestNotFoundException;
import com.group5.ems.exception.WorkflowException;
import com.group5.ems.repository.BenefitTypeRepository;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeLeaveBalanceRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.PositionRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final BenefitTypeRepository benefitTypeRepository;
    private final ApprovalWorkflowService workflowService;
    private final EmployeeLeaveBalanceRepository leaveBalanceRepository;
    private final LogService logService;

    private static final int MIN_REJECTION_REASON_LENGTH = 10;
    private static final DateTimeFormatter DTF_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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

        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) return Page.empty();

        Employee currentEmployee = employeeRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("No employee record found for current user"));

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
    public void createRequest(Long requestTypeId, String title, String content, boolean urgent) {
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
        request.setUrgent(urgent);
        request.setPriority(urgent ? "URGENT" : "NORMAL");
        
        // Option B: Skip DM/HR -> DIRECT TO HRM
        request.setStatus(WorkflowConstants.STATUS_PENDING);
        request.setStep(WorkflowConstants.STEP_WAITING_HRM);
        
        Request saved = requestRepository.save(request);

        saveHistory(saved.getId(), currentUserId, "SUBMITTED", "Created by HR - Escalated to HR Manager" + (urgent ? " (URGENT)" : ""));
        logService.log(AuditAction.CREATE, AuditEntityType.REQUEST, saved.getId());
    }

    // ══════════════════════════════════════════════════════════════════
    // SUBMIT RECRUITMENT / ONBOARDING TICKET
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public void submitRecruitmentTicket(RecruitmentTicketDTO dto) {
        // 1. Fetch the REC_ACCOUNT request type
        RequestType requestType = requestTypeRepository.findByCode("REC_ACCOUNT")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Request type 'REC_ACCOUNT' not found. Please seed it in the database."));

        // 2. Resolve current HR employee
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("Cannot determine the current logged-in user.");
        }
        Employee hrEmployee = employeeRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No employee record found for current user ID: " + currentUserId));

        // 3. Resolve names from IDs for a human-readable ticket
        String departmentName = departmentRepository.findById(dto.departmentId())
                .map(Department::getName)
                .orElse("Unknown (ID: " + dto.departmentId() + ")");

        String positionName = positionRepository.findById(dto.positionId())
                .map(Position::getName)
                .orElse("Unknown (ID: " + dto.positionId() + ")");

        String managerName = "N/A";
        if (dto.reportingManagerId() != null) {
            managerName = employeeRepository.findByIdWithDetails(dto.reportingManagerId())
                    .map(e -> e.getUser() != null ? e.getUser().getFullName() : e.getEmployeeCode())
                    .orElse("Unknown (ID: " + dto.reportingManagerId() + ")");
        }

        // 4. Resolve selected benefit type names
        String bonusNames = "None";
        if (dto.bonusIds() != null && !dto.bonusIds().isEmpty()) {
            List<BenefitType> selectedBenefits = benefitTypeRepository.findAllById(dto.bonusIds());
            bonusNames = selectedBenefits.stream()
                    .map(BenefitType::getName)
                    .collect(Collectors.joining(", "));
        }

        // 5. Build the full name
        String fullName = dto.firstName().trim() + " " + dto.lastName().trim();

        // 6. Compose the formatted content string
        StringBuilder content = new StringBuilder();
        content.append("═══ ONBOARDING REQUEST ═══\n\n");
        content.append("── Candidate Details ──\n");
        content.append("Full Name: ").append(fullName).append("\n");
        content.append("Email: ").append(dto.email()).append("\n");
        content.append("Phone: ").append(dto.phone()).append("\n\n");
        content.append("── Employment Info ──\n");
        content.append("Department: ").append(departmentName).append("\n");
        content.append("Position: ").append(positionName).append("\n");
        content.append("Joining Date: ").append(dto.joiningDate()).append("\n");
        content.append("Reporting Manager: ").append(managerName).append("\n\n");
        content.append("── Compensation ──\n");
        content.append("Base Salary: ").append(dto.baseSalary()).append("\n");
        content.append("Pay Frequency: ").append(dto.payFrequency()).append("\n");
        content.append("Benefits/Bonuses: ").append(bonusNames).append("\n\n");
        if (dto.additionalNotes() != null && !dto.additionalNotes().isBlank()) {
            content.append("── Additional Notes ──\n");
            content.append(dto.additionalNotes().trim()).append("\n");
        }

        // 7. Create and save the Request entity
        Request request = new Request();
        request.setEmployeeId(hrEmployee.getId());
        request.setRequestTypeId(requestType.getId());
        request.setTitle("Onboarding Request: " + fullName);
        request.setContent(content.toString());

        // HR-created tickets skip DM/HR and go directly to HRM
        request.setStatus(WorkflowConstants.STATUS_PENDING);
        request.setStep(WorkflowConstants.STEP_WAITING_HRM);

        Request saved = requestRepository.save(request);

        saveHistory(saved.getId(), currentUserId, "SUBMITTED",
                "Onboarding ticket created by HR - Escalated to HR Manager");
        logService.log(AuditAction.CREATE, AuditEntityType.REQUEST, saved.getId());
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

    public Map<String, List<RequestType>> getGroupedRequestTypes() {
        return getCreatableRequestTypes().stream()
                .collect(Collectors.groupingBy(
                        RequestType::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
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
        String avatarUrl = null;
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
                avatarUrl = emp.getUser().getAvatarUrl();
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

        String approverEmployeeCode = null;
        if (request.getApprovedByUser() != null && request.getApprovedByUser().getEmployee() != null) {
            approverEmployeeCode = request.getApprovedByUser().getEmployee().getEmployeeCode();
        }

        String statusClass = "border-slate-200 bg-slate-50 text-slate-500";
        String statusDisplay = request.getStatus();

        if (WorkflowConstants.STATUS_PENDING.equals(request.getStatus())) {
            statusClass = "border-amber-200 bg-amber-50 text-amber-600";
            statusDisplay = "Pending Review";
        } else if (WorkflowConstants.STATUS_APPROVED.equals(request.getStatus())) {
            statusClass = "border-emerald-200 bg-emerald-50 text-emerald-600";
            statusDisplay = "Approved";
        } else if (WorkflowConstants.STATUS_REJECTED.equals(request.getStatus())) {
            statusClass = "border-rose-200 bg-rose-50 text-rose-600";
            statusDisplay = "Rejected";
        }

        // Position
        String empPosition = "Employee";
        if (request.getEmployee() != null && request.getEmployee().getPosition() != null) {
            empPosition = request.getEmployee().getPosition().getName();
        }

        // Leave specific metadata
        BigDecimal balanceRemaining = null;
        BigDecimal balanceTotal = null;
        Integer balancePercentage = null;
        Integer overlapCount = null;
        boolean isLeaveRequest = request.getRequestType() != null && "ATTENDANCE".equals(request.getRequestType().getCategory());

        if (isLeaveRequest) {
            int currentYear = LocalDate.now().getYear();
            var balanceOpt = leaveBalanceRepository.findByEmployeeIdAndYear(request.getEmployeeId(), currentYear);
            if (balanceOpt.isPresent()) {
                var balance = balanceOpt.get();
                balanceRemaining = balance.getRemainingDays();
                balanceTotal = balance.getTotalDays();
                if (balanceTotal != null && balanceTotal.compareTo(BigDecimal.ZERO) > 0) {
                    balancePercentage = balanceRemaining.multiply(new BigDecimal(100))
                            .divide(balanceTotal, 0, java.math.RoundingMode.HALF_UP).intValue();
                }
            }

            if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
                var overlaps = requestRepository.findOverlappingLeaveRequests("APPROVED", request.getLeaveFrom(), request.getLeaveTo());
                overlapCount = (int) overlaps.stream()
                        .filter(r -> !r.getId().equals(request.getId()))
                        .count();
            }
        }

        return HrRequestDTO.builder()
                .id(request.getId())
                .requestedBy(empName)
                .initials(initials)
                .avatarUrl(avatarUrl)
                .department(deptName)
                .departmentId(deptId)
                .employeeCode(empCode)
                .employeePosition(empPosition)
                .category(category)
                .categoryCode(categoryCode)
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus())
                .rejectedReason(request.getRejectedReason())
                .submittedAt(request.getCreatedAt())
                .submittedAtDisplay(request.getCreatedAt() != null ? request.getCreatedAt().format(DTF_FULL) : "N/A")
                .processedAt(processedAt)
                .approverName(approverName)
                .approverEmployeeCode(approverEmployeeCode)
                .statusClass(statusClass)
                .statusDisplay(statusDisplay)
                .leaveBalanceRemaining(balanceRemaining)
                .leaveBalanceTotal(balanceTotal)
                .leaveBalancePercentage(balancePercentage)
                .overlapCount(overlapCount)
                .isLeaveRequest(isLeaveRequest)
                .urgentFlag(request.isUrgent())
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
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }
}