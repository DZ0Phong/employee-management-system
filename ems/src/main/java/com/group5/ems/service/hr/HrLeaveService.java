package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrLeaveBalanceSummaryDTO;
import com.group5.ems.dto.response.HrLeaveCalendarEventDTO;
import com.group5.ems.dto.response.HrLeaveRequestDTO;
import com.group5.ems.dto.response.HrLeaveStatsDTO;
import com.group5.ems.entity.Request;
import com.group5.ems.constants.WorkflowConstants;
import com.group5.ems.entity.User;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.exception.InvalidRejectionReasonException;
import com.group5.ems.exception.LeaveRequestNotFoundException;
import com.group5.ems.exception.WorkflowException;
import com.group5.ems.repository.EmployeeLeaveBalanceRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.common.ApprovalWorkflowService;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrLeaveService {

    private final RequestRepository requestRepository;
    private final EmployeeLeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;
    private final ApprovalWorkflowService workflowService;
    private final LogService logService;

    private static final int MIN_REJECTION_REASON_LENGTH = 10;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DTF_FULL = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ── Rejection reason categories ──
    private static final Map<String, String> REJECTION_CATEGORIES = new HashMap<>();
    static {
        REJECTION_CATEGORIES.put("INSUFFICIENT_COVERAGE", "Insufficient Coverage");
        REJECTION_CATEGORIES.put("BLACKOUT_PERIOD", "Blackout Period");
        REJECTION_CATEGORIES.put("POLICY_CONFLICT", "Policy Conflict");
        REJECTION_CATEGORIES.put("BALANCE_EXCEEDED", "Leave Balance Exceeded");
        REJECTION_CATEGORIES.put("SCHEDULING_CONFLICT", "Scheduling Conflict");
        REJECTION_CATEGORIES.put("DOCUMENTATION_MISSING", "Documentation Missing");
        REJECTION_CATEGORIES.put("OTHER", "Other");
    }

    /**
     * Returns the predefined rejection reason categories for the UI dropdown.
     */
    public Map<String, String> getRejectionCategories() {
        return REJECTION_CATEGORIES;
    }

    // ══════════════════════════════════════════════════════════════════
    // PENDING LEAVES (always full list, small count)
    // ══════════════════════════════════════════════════════════════════

    public List<HrLeaveRequestDTO> getPendingLeaves() {
        return requestRepository.findPendingLeaveRequests().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<HrLeaveRequestDTO> getHrmPendingLeaves() {
        return requestRepository.findHrmPendingLeaveRequests().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════
    // LEAVE HISTORY — server-side filtered + paginated
    // ══════════════════════════════════════════════════════════════════

    public Page<HrLeaveRequestDTO> getLeaveHistory(Pageable pageable) {
        Page<Request> page = requestRepository.findLeaveHistory(pageable);
        List<HrLeaveRequestDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    /**
     * Server-side filtered leave history (improvement #2).
     */
    public Page<HrLeaveRequestDTO> getLeaveHistoryFiltered(
            String status, Long departmentId, String leaveType,
            String search, LocalDate dateFrom, LocalDate dateTo,
            Pageable pageable) {

        // Normalize blank strings to null for JPQL IS NULL checks
        status = normalizeBlank(status);
        leaveType = normalizeBlank(leaveType);
        search = normalizeBlank(search);

        Page<Request> page = requestRepository.findLeaveHistoryFiltered(
                status, departmentId, leaveType, search, dateFrom, dateTo, pageable);
        List<HrLeaveRequestDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // ══════════════════════════════════════════════════════════════════
    // APPROVE / REJECT with validation + audit logging
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public void approveLeave(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException(id));

        if (!workflowService.canApprove(request, WorkflowConstants.ROLE_HR)) {
            throw new WorkflowException("Cannot approve: Must be approved by DM first");
        }

        Long currentUserId = getCurrentUserId();
        workflowService.moveToNextStep(request, currentUserId, WorkflowConstants.ROLE_HR);
        
        logService.log(AuditAction.UPDATE, AuditEntityType.LEAVE, id);
    }

    @Transactional
    public void rejectLeave(Long id, String category, String reason) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException(id));

        if (!workflowService.canApprove(request, WorkflowConstants.ROLE_HR)) {
            throw new WorkflowException("Cannot reject at current step");
        }

        // Build full rejection reason from category + description
        String fullReason = buildRejectionReason(category, reason);
        validateRejectionReason(fullReason);

        Long currentUserId = getCurrentUserId();
        workflowService.rejectRequest(request, currentUserId, WorkflowConstants.ROLE_HR, fullReason);

        logService.log(AuditAction.UPDATE, AuditEntityType.LEAVE, id);
    }

    // ══════════════════════════════════════════════════════════════════
    // BULK APPROVE / REJECT (improvement #5)
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public int bulkApprove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;

        List<Request> requests = requestRepository.findPendingLeavesByIds(ids);
        Long currentUserId = getCurrentUserId();
        int count = 0;

        for (Request request : requests) {
            if (workflowService.canApprove(request, WorkflowConstants.ROLE_HR)) {
                workflowService.moveToNextStep(request, currentUserId, WorkflowConstants.ROLE_HR);
                logService.log(AuditAction.UPDATE, AuditEntityType.LEAVE, request.getId());
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

        List<Request> requests = requestRepository.findPendingLeavesByIds(ids);
        Long currentUserId = getCurrentUserId();
        int count = 0;

        for (Request request : requests) {
            if (workflowService.canApprove(request, WorkflowConstants.ROLE_HR)) {
                workflowService.rejectRequest(request, currentUserId, WorkflowConstants.ROLE_HR, fullReason);
                logService.log(AuditAction.UPDATE, AuditEntityType.LEAVE, request.getId());
                count++;
            }
        }
        return count;
    }

    // ══════════════════════════════════════════════════════════════════
    // STATISTICS (improvement #6)
    // ══════════════════════════════════════════════════════════════════

    public HrLeaveStatsDTO getLeaveStats() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long totalPending = requestRepository.countByStatusAndStepWaitingHRAndRequestTypeCodeIn(
                "PENDING",
                List.of("LEAVE_ANNUAL", "LEAVE_SICK", "LEAVE_UNPAID", "LV_ANNUAL", "LV_SICK"));

        long approvedThisMonth = requestRepository.countLeaveByStatusSince("APPROVED", monthStart);
        long rejectedThisMonth = requestRepository.countLeaveByStatusSince("REJECTED", monthStart);
        long onLeaveToday = requestRepository.countOnLeaveToday();

        Double avgHours = requestRepository.avgProcessingHoursSince(monthStart);
        double avgProcessingHours = avgHours != null ? avgHours : 0.0;

        // Top leave type
        List<Object[]> topTypes = requestRepository.findTopLeaveTypes();
        String topLeaveType = "N/A";
        long topLeaveTypeCount = 0;
        if (!topTypes.isEmpty()) {
            Object[] top = topTypes.get(0);
            String code = (String) top[0];
            topLeaveType = REJECTION_CATEGORIES.getOrDefault(code, formatLeaveTypeCode(code));
            topLeaveTypeCount = ((Number) top[1]).longValue();
        }

        return HrLeaveStatsDTO.builder()
                .totalPending(totalPending)
                .approvedThisMonth(approvedThisMonth)
                .rejectedThisMonth(rejectedThisMonth)
                .onLeaveToday(onLeaveToday)
                .avgProcessingHours(avgProcessingHours)
                .topLeaveType(topLeaveType)
                .topLeaveTypeCount(topLeaveTypeCount)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // LEAVE BALANCE SUMMARY (improvement #1)
    // ══════════════════════════════════════════════════════════════════

    public HrLeaveBalanceSummaryDTO getLeaveBalanceSummary() {
        int currentYear = LocalDate.now().getYear();

        BigDecimal totalAllocated = leaveBalanceRepository.sumTotalDaysByYear(currentYear);
        BigDecimal totalUsed = leaveBalanceRepository.sumUsedDaysByYear(currentYear);
        BigDecimal totalPending = leaveBalanceRepository.sumPendingDaysByYear(currentYear);
        BigDecimal totalRemaining = leaveBalanceRepository.sumRemainingDaysByYear(currentYear);
        long employeeCount = leaveBalanceRepository.countByYear(currentYear);

        double avgUtilization = 0.0;
        if (totalAllocated.compareTo(BigDecimal.ZERO) > 0) {
            avgUtilization = totalUsed.divide(totalAllocated, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return HrLeaveBalanceSummaryDTO.builder()
                .totalDaysAllocated(totalAllocated)
                .totalDaysUsed(totalUsed)
                .totalDaysPending(totalPending)
                .totalDaysRemaining(totalRemaining)
                .employeesWithBalance(employeeCount)
                .avgUtilizationPercent(avgUtilization)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // CALENDAR EVENTS (improvement #3)
    // ══════════════════════════════════════════════════════════════════

    public List<HrLeaveCalendarEventDTO> getCalendarEvents(LocalDate start, LocalDate end) {
        return requestRepository.findCalendarEvents(start, end).stream()
                .map(this::mapToCalendarEvent)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════
    // CSV EXPORT (improvement #7)
    // ══════════════════════════════════════════════════════════════════

    public void exportLeaveHistoryToCsv(String status, Long departmentId, PrintWriter writer) {
        status = normalizeBlank(status);

        List<Request> requests = requestRepository.findLeaveHistoryForExport(status, departmentId);

        // CSV header
        writer.println("Employee Name,Employee Code,Department,Leave Type,From,To,Duration,Status,Rejection Reason,Processed At");

        for (Request r : requests) {
            HrLeaveRequestDTO dto = mapToDTO(r);
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    escapeCsv(dto.employeeName()),
                    escapeCsv(dto.employeeCode()),
                    escapeCsv(dto.department()),
                    escapeCsv(dto.leaveType()),
                    dto.leave_from() != null ? dto.leave_from().toString() : "",
                    dto.leave_to() != null ? dto.leave_to().toString() : "",
                    escapeCsv(dto.duration()),
                    escapeCsv(dto.status()),
                    escapeCsv(dto.rejectedReason() != null ? dto.rejectedReason() : ""),
                    escapeCsv(dto.processedAt() != null ? dto.processedAt() : ""));
        }
        writer.flush();
    }

    // ══════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════

    private HrLeaveRequestDTO mapToDTO(Request request) {
        String initials = "";
        String fullName = "Unknown";
        String departmentName = "N/A";
        String employeeCode = "N/A";
        Long departmentId = null;

        if (request.getEmployee() != null) {
            if (request.getEmployee().getUser() != null) {
                fullName = request.getEmployee().getUser().getFullName();
                if (fullName != null && !fullName.trim().isEmpty()) {
                    String[] names = fullName.trim().split("\\s+");
                    initials += names[0].charAt(0);
                    if (names.length > 1) {
                        initials += names[names.length - 1].charAt(0);
                    }
                }
            }
            if (request.getEmployee().getDepartment() != null) {
                departmentName = request.getEmployee().getDepartment().getName();
                departmentId = request.getEmployee().getDepartment().getId();
            }
            if (request.getEmployee().getEmployeeCode() != null) {
                employeeCode = request.getEmployee().getEmployeeCode();
            }
        }

        String duration = "N/A";
        String dates = "N/A";

        if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
            long days = ChronoUnit.DAYS.between(request.getLeaveFrom(), request.getLeaveTo()) + 1;
            duration = days + (days == 1 ? " Day" : " Days");
            dates = request.getLeaveFrom().format(DTF) + " - " + request.getLeaveTo().format(DTF);
        } else if (request.getLeaveFrom() != null) {
            duration = "1 Day";
            dates = request.getLeaveFrom().format(DTF);
        }

        String reason = request.getContent() != null ? request.getContent() : "No reason provided";

        // Approver name (improvement #8)
        String approverName = "HR";
        if (request.getApprovedByUser() != null && request.getApprovedByUser().getFullName() != null) {
            approverName = request.getApprovedByUser().getFullName();
        }

        // Processed timestamp
        String processedAt = null;
        if (request.getApprovedAt() != null) {
            processedAt = request.getApprovedAt().format(DTF_FULL);
        }

        return HrLeaveRequestDTO.builder()
                .id(request.getId())
                .employeeName(fullName)
                .initials(initials.toUpperCase())
                .department(departmentName)
                .departmentId(departmentId)
                .employeeCode(employeeCode)
                .leaveType(request.getRequestType() != null ? request.getRequestType().getName() : "N/A")
                .duration(duration)
                .dates(dates)
                .reason(reason)
                .leave_from(request.getLeaveFrom())
                .leave_to(request.getLeaveTo())
                .status(request.getStatus())
                .rejectedReason(request.getRejectedReason())
                .processedAt(processedAt)
                .approverName(approverName)
                .build();
    }

    private HrLeaveCalendarEventDTO mapToCalendarEvent(Request request) {
        String employeeName = "Unknown";
        String departmentName = "N/A";
        if (request.getEmployee() != null && request.getEmployee().getUser() != null) {
            employeeName = request.getEmployee().getUser().getFullName();
        }
        if (request.getEmployee() != null && request.getEmployee().getDepartment() != null) {
            departmentName = request.getEmployee().getDepartment().getName();
        }

        String leaveTypeName = request.getRequestType() != null ? request.getRequestType().getName() : "Leave";
        String title = employeeName + " — " + leaveTypeName;

        // Color coding by status
        String color;
        String textColor = "#ffffff";
        String borderColor;
        if ("APPROVED".equals(request.getStatus())) {
            color = "#10b981";      // emerald-500
            borderColor = "#059669"; // emerald-600
        } else {
            color = "#f59e0b";       // amber-500
            borderColor = "#d97706"; // amber-600
        }

        // FullCalendar end date is exclusive, so add 1 day
        LocalDate endExclusive = request.getLeaveTo() != null
                ? request.getLeaveTo().plusDays(1) : request.getLeaveFrom().plusDays(1);

        return HrLeaveCalendarEventDTO.builder()
                .id(request.getId())
                .title(title)
                .start(request.getLeaveFrom().toString())
                .end(endExclusive.toString())
                .color(color)
                .textColor(textColor)
                .borderColor(borderColor)
                .department(departmentName)
                .leaveType(leaveTypeName)
                .status(request.getStatus())
                .build();
    }

    private String buildRejectionReason(String category, String description) {
        if (category == null || category.isBlank() || "OTHER".equals(category)) {
            return description != null ? description.trim() : "";
        }
        String categoryLabel = REJECTION_CATEGORIES.getOrDefault(category, category);
        String desc = (description != null && !description.isBlank()) ? ": " + description.trim() : "";
        return categoryLabel + desc;
    }

    private void validateRejectionReason(String reason) {
        if (reason == null || reason.trim().length() < MIN_REJECTION_REASON_LENGTH) {
            throw new InvalidRejectionReasonException();
        }
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

    private String normalizeBlank(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private String formatLeaveTypeCode(String code) {
        if (code == null) return "N/A";
        return code.replace("_", " ")
                .replace("LEAVE ", "")
                .substring(0, 1).toUpperCase() + code.replace("_", " ").substring(1).toLowerCase();
    }
}