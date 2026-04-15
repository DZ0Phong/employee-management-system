package com.group5.ems.service.employee;

import com.group5.ems.constants.WorkflowConstants;
import com.group5.ems.dto.request.CreateLeaveRequestDTO;
import com.group5.ems.dto.response.LeaveBalanceDTO;
import com.group5.ems.dto.response.LeaveRequestDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.RequestApprovalHistory;
import com.group5.ems.entity.RequestType;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.RequestApprovalHistoryRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.RequestTypeRepository;
import com.group5.ems.service.common.LogService;
import com.group5.ems.util.WorkingDayUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl {
    private static final String ATTENDANCE_CATEGORY = "ATTENDANCE";

    private final EmployeeRepository employeeRepository;
    private final RequestRepository requestRepository;
    private final RequestApprovalHistoryRepository requestApprovalHistoryRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final LogService logService;

    @Transactional(readOnly = true)
    public List<LeaveBalanceDTO> getLeaveBalances(Long employeeId) {
        List<Request> allLeaves = requestRepository.findByEmployeeIdAndLeaveTypeIsNotNull(employeeId);

        return getSupportedLeaveTypes().stream()
                .map(requestType -> buildBalanceForRequestType(requestType, allLeaves))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDTO> getLeaveHistory(Long employeeId) {
        return requestRepository.findByEmployeeIdAndLeaveTypeIsNotNull(employeeId)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createLeaveRequest(Long employeeId, CreateLeaveRequestDTO dto) {
        if (dto == null || dto.getLeaveType() == null || dto.getLeaveType().isBlank()) {
            throw new RuntimeException("Please select a leave type.");
        }
        // ── Validate ngày ──────────────────────────────────
        if (dto.getLeaveFrom() == null || dto.getLeaveTo() == null) {
            throw new RuntimeException("Please select both start and end dates.");
        }

        if (dto.getLeaveFrom().isBefore(LocalDate.now())) {
            throw new RuntimeException("Leave start date cannot be in the past.");
        }

        if (dto.getLeaveTo().isBefore(dto.getLeaveFrom())) {
            throw new RuntimeException("End date must be after or equal to start date.");
        }

        // ── Kiểm tra trùng ngày ────────────────────────────
        List<Request> existing = requestRepository.findByEmployeeIdAndLeaveTypeIsNotNull(employeeId);
        boolean overlap = existing.stream()
                .filter(r -> !WorkflowConstants.STATUS_REJECTED.equalsIgnoreCase(r.getStatus()))
                .filter(r -> !WorkflowConstants.STATUS_CANCELLED.equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getLeaveFrom() != null && r.getLeaveTo() != null)
                .anyMatch(r ->
                        !dto.getLeaveFrom().isAfter(r.getLeaveTo()) &&
                                !dto.getLeaveTo().isBefore(r.getLeaveFrom())
                );

        if (overlap) {
            throw new RuntimeException("You already have a leave request overlapping this period.");
        }

        // ── Kiểm tra balance ───────────────────────────────
        long requestedDays = WorkingDayUtils.countWorkingDays(dto.getLeaveFrom(), dto.getLeaveTo());
        if (requestedDays <= 0) {
            throw new RuntimeException("Selected leave range must contain at least one working day.");
        }
        List<LeaveBalanceDTO> balances = getLeaveBalances(employeeId);

        String normalizedLeaveType = normalizeLeaveType(dto.getLeaveType());
        LeaveBalanceDTO balance = balances.stream()
                .filter(b -> b.getLeaveType().equals(normalizedLeaveType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported leave type."));

        if (!balance.isUnlimited() && !balance.isRequestable()) {
            throw new RuntimeException("This leave type has no remaining days.");
        }

        if (!balance.isUnlimited() && requestedDays > balance.getRemainingDays()) {
            throw new RuntimeException("Insufficient leave balance. You have "
                    + (int) balance.getRemainingDays() + " day(s) remaining.");
        }

        // ── Map leaveType -> request_type code ────────────
        String requestTypeCode = toRequestTypeCode(normalizedLeaveType);

        RequestType requestType = requestTypeRepository.findByCode(requestTypeCode)
                .orElseThrow(() -> new RuntimeException("Request type not found: " + requestTypeCode));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found."));
        boolean autoForwardToHr = hasDeptManagerRole(employee);

        // ── Tạo request ────────────────────────────────────
        Request request = new Request();
        request.setEmployeeId(employeeId);
        request.setRequestTypeId(requestType.getId());
        request.setLeaveType(normalizedLeaveType);
        request.setLeaveFrom(dto.getLeaveFrom());
        request.setLeaveTo(dto.getLeaveTo());
        request.setContent(dto.getContent());
        request.setTitle(requestType.getName() + " Request");
        request.setUrgent(dto.isUrgent());
        request.setStatus("PENDING");
        request.setStep(autoForwardToHr ? WorkflowConstants.STEP_WAITING_HR : WorkflowConstants.STEP_WAITING_DM);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        Request savedRequest = requestRepository.save(request);
        saveSubmissionHistory(savedRequest.getId(), employee.getUserId(), autoForwardToHr);
        logService.log(AuditAction.CREATE, AuditEntityType.LEAVE, savedRequest.getId());
    }

    @Transactional
    public void cancelLeaveRequest(Long employeeId, Long requestId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found."));
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found."));

        if (!employeeId.equals(request.getEmployeeId())) {
            throw new RuntimeException("You are not allowed to cancel this leave request.");
        }

        if (!isCancelable(request)) {
            throw new RuntimeException("This leave request can no longer be cancelled.");
        }

        request.setStatus(WorkflowConstants.STATUS_CANCELLED);
        request.setStep(WorkflowConstants.STEP_CANCELLED);
        request.setCurrentApproverId(null);
        request.setApprovedAt(null);
        request.setApprovedBy(null);

        Request savedRequest = requestRepository.save(request);
        saveCancellationHistory(savedRequest.getId(), employee.getUserId());
        logService.log(AuditAction.UPDATE, AuditEntityType.LEAVE, savedRequest.getId());
    }

    // ── Helper methods ──────────────────────────────────────

    private LeaveBalanceDTO buildBalance(String leaveType, double total, List<Request> allLeaves) {
        double approved = allLeaves.stream()
                .filter(r -> leaveType.equals(normalizeLeaveType(r.getLeaveType())))
                .filter(r -> WorkflowConstants.STATUS_APPROVED.equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getLeaveFrom() != null && r.getLeaveTo() != null)
                .mapToDouble(r -> WorkingDayUtils.countWorkingDays(r.getLeaveFrom(), r.getLeaveTo()))
                .sum();

        double pending = allLeaves.stream()
                .filter(r -> leaveType.equals(normalizeLeaveType(r.getLeaveType())))
                .filter(r -> WorkflowConstants.STATUS_PENDING.equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getLeaveFrom() != null && r.getLeaveTo() != null)
                .mapToDouble(r -> WorkingDayUtils.countWorkingDays(r.getLeaveFrom(), r.getLeaveTo()))
                .sum();

        double reserved = approved + pending;
        double remaining = Math.max(0, total - reserved);
        double usagePercentage = total > 0 ? Math.round((reserved / total) * 100) : 0;

        return LeaveBalanceDTO.builder()
                .leaveType(leaveType)
                .totalDays(total)
                .usedDays(approved)
                .pendingDays(pending)
                .remainingDays(remaining)
                .usagePercentage(usagePercentage)
                .requestable(remaining > 0)
                .unlimited(false)
                .build();
    }

    private LeaveBalanceDTO buildUnlimitedBalance(String leaveType, List<Request> allLeaves) {
        double approved = allLeaves.stream()
                .filter(r -> leaveType.equals(normalizeLeaveType(r.getLeaveType())))
                .filter(r -> WorkflowConstants.STATUS_APPROVED.equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getLeaveFrom() != null && r.getLeaveTo() != null)
                .mapToDouble(r -> WorkingDayUtils.countWorkingDays(r.getLeaveFrom(), r.getLeaveTo()))
                .sum();

        double pending = allLeaves.stream()
                .filter(r -> leaveType.equals(normalizeLeaveType(r.getLeaveType())))
                .filter(r -> WorkflowConstants.STATUS_PENDING.equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getLeaveFrom() != null && r.getLeaveTo() != null)
                .mapToDouble(r -> WorkingDayUtils.countWorkingDays(r.getLeaveFrom(), r.getLeaveTo()))
                .sum();

        return LeaveBalanceDTO.builder()
                .leaveType(leaveType)
                .totalDays(0)
                .usedDays(approved)
                .pendingDays(pending)
                .remainingDays(0)
                .usagePercentage(0)
                .requestable(true)
                .unlimited(true)
                .build();
    }

    private LeaveBalanceDTO buildBalanceForRequestType(RequestType requestType, List<Request> allLeaves) {
        String leaveType = normalizeLeaveType(requestType.getCode());
        if ("UNPAID_LEAVE".equals(leaveType)) {
            return buildUnlimitedBalance(leaveType, allLeaves);
        }
        return buildBalance(leaveType, getDefaultQuota(leaveType), allLeaves);
    }

    private LeaveRequestDTO mapToDTO(Request req) {
        return LeaveRequestDTO.builder()
                .id(req.getId())
                .leaveType(req.getLeaveType())
                .leaveFrom(req.getLeaveFrom())
                .leaveTo(req.getLeaveTo())
                .content(req.getContent())
                .urgent(req.isUrgent())
                .status(req.getStatus())
                .rejectedReason(req.getRejectedReason())
                .step(req.getStep())
                .statusDisplay(resolveStatusDisplay(req))
                .stepDisplay(resolveStepDisplay(req.getStep()))
                .createdAt(req.getCreatedAt())
                .cancelable(isCancelable(req))
                .build();
    }

    private String resolveStatusDisplay(Request request) {
        if (request == null) {
            return "Unknown";
        }
        if ("APPROVED".equalsIgnoreCase(request.getStatus())) {
            return "Approved";
        }
        if ("REJECTED".equalsIgnoreCase(request.getStatus())) {
            return "Rejected";
        }
        if (WorkflowConstants.STATUS_CANCELLED.equalsIgnoreCase(request.getStatus())) {
            return "Cancelled";
        }

        String step = request.getStep() != null ? request.getStep() : WorkflowConstants.STEP_WAITING_DM;
        return switch (step) {
            case WorkflowConstants.STEP_WAITING_HR -> "Waiting for HR";
            case WorkflowConstants.STEP_WAITING_HRM -> "Waiting for HR Manager";
            default -> "Waiting for Department Manager";
        };
    }

    private String resolveStepDisplay(String step) {
        String normalized = step != null ? step : WorkflowConstants.STEP_WAITING_DM;
        return switch (normalized) {
            case WorkflowConstants.STEP_WAITING_HR -> "HR validation";
            case WorkflowConstants.STEP_WAITING_HRM -> "HR Manager final decision";
            case WorkflowConstants.STEP_COMPLETED -> "Completed";
            case WorkflowConstants.STEP_REJECTED -> "Rejected";
            case WorkflowConstants.STEP_CANCELLED -> "Cancelled by employee";
            default -> "Department Manager review";
        };
    }

    private boolean isCancelable(Request request) {
        if (request == null || !WorkflowConstants.STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            return false;
        }

        String step = request.getStep() != null
                ? request.getStep().toUpperCase(Locale.ROOT)
                : WorkflowConstants.STEP_WAITING_DM;

        return WorkflowConstants.STEP_WAITING_DM.equals(step)
                || WorkflowConstants.STEP_WAITING_HR.equals(step)
                || WorkflowConstants.STEP_WAITING_HRM.equals(step);
    }

    private void saveCancellationHistory(Long requestId, Long userId) {
        if (userId == null) {
            return;
        }

        RequestApprovalHistory history = new RequestApprovalHistory();
        history.setRequestId(requestId);
        history.setApproverId(userId);
        history.setAction("CANCELLED");
        history.setComment("Cancelled by employee");
        requestApprovalHistoryRepository.save(history);
    }


    private void saveSubmissionHistory(Long requestId, Long userId, boolean autoForwardedToHr) {
        if (userId == null) {
            return;
        }

        RequestApprovalHistory history = new RequestApprovalHistory();
        history.setRequestId(requestId);
        history.setApproverId(userId);
        history.setAction(autoForwardedToHr ? "AUTO_FORWARDED_TO_HR" : "SUBMITTED");
        history.setComment(autoForwardedToHr
                ? "Submitted by Department Manager and automatically forwarded to HR"
                : "Submitted by employee");
        requestApprovalHistoryRepository.save(history);
    }

    private boolean hasDeptManagerRole(Employee employee) {
        return employee != null
                && employee.getUser() != null
                && employee.getUser().getUserRoles() != null
                && employee.getUser().getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole() != null
                        && WorkflowConstants.ROLE_DEPT_MANAGER.equalsIgnoreCase(userRole.getRole().getCode()));
    }

    private String normalizeLeaveType(String leaveType) {
        if (leaveType == null) {
            return "";
        }
        return switch (leaveType.trim().toUpperCase(Locale.ROOT)) {
            case "PERSONAL_LEAVE", "UNPAID_LEAVE", "LEAVE_UNPAID" -> "UNPAID_LEAVE";
            case "LEAVE_SICK", "SICK_LEAVE" -> "SICK_LEAVE";
            case "LEAVE_BEREAVEMENT", "BEREAVEMENT_LEAVE" -> "BEREAVEMENT_LEAVE";
            case "LEAVE_STUDY", "STUDY_LEAVE" -> "STUDY_LEAVE";
            case "LEAVE_MATERNITY", "MATERNITY_LEAVE" -> "MATERNITY_LEAVE";
            case "LEAVE_PATERNITY", "PATERNITY_LEAVE" -> "PATERNITY_LEAVE";
            case "LEAVE_ANNUAL", "ANNUAL_LEAVE" -> "ANNUAL_LEAVE";
            default -> leaveType.trim().toUpperCase(Locale.ROOT);
        };
    }

    public List<RequestType> getSupportedLeaveTypes() {
        return requestTypeRepository.findByCategoryAndCodeStartingWithOrderByNameAsc(ATTENDANCE_CATEGORY, "LEAVE_")
                .stream()
                .filter(requestType -> requestType.getCode() != null)
                .sorted(Comparator.comparingInt(requestType -> sortOrderForLeaveCode(requestType.getCode())))
                .collect(Collectors.toList());
    }

    private String toRequestTypeCode(String normalizedLeaveType) {
        return switch (normalizedLeaveType) {
            case "ANNUAL_LEAVE" -> "LEAVE_ANNUAL";
            case "SICK_LEAVE" -> "LEAVE_SICK";
            case "UNPAID_LEAVE" -> "LEAVE_UNPAID";
            case "MATERNITY_LEAVE" -> "LEAVE_MATERNITY";
            case "PATERNITY_LEAVE" -> "LEAVE_PATERNITY";
            case "BEREAVEMENT_LEAVE" -> "LEAVE_BEREAVEMENT";
            case "STUDY_LEAVE" -> "LEAVE_STUDY";
            default -> throw new RuntimeException("Unsupported leave type: " + normalizedLeaveType);
        };
    }

    private double getDefaultQuota(String leaveType) {
        return switch (leaveType) {
            case "ANNUAL_LEAVE" -> 12.0;
            case "SICK_LEAVE" -> 10.0;
            case "MATERNITY_LEAVE" -> 180.0;
            case "PATERNITY_LEAVE" -> 7.0;
            case "BEREAVEMENT_LEAVE" -> 3.0;
            case "STUDY_LEAVE" -> 10.0;
            default -> 0.0;
        };
    }

    private int sortOrderForLeaveCode(String code) {
        return switch (normalizeLeaveType(code)) {
            case "ANNUAL_LEAVE" -> 1;
            case "SICK_LEAVE" -> 2;
            case "UNPAID_LEAVE" -> 3;
            case "MATERNITY_LEAVE" -> 4;
            case "PATERNITY_LEAVE" -> 5;
            case "BEREAVEMENT_LEAVE" -> 6;
            case "STUDY_LEAVE" -> 7;
            default -> 99;
        };
    }
}

