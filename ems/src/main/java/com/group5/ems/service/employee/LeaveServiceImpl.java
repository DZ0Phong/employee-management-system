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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl {

    private final EmployeeRepository employeeRepository;
    private final RequestRepository requestRepository;
    private final RequestApprovalHistoryRepository requestApprovalHistoryRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final LogService logService;

    @Transactional(readOnly = true)
    public List<LeaveBalanceDTO> getLeaveBalances(Long employeeId) {
        List<Request> allLeaves = requestRepository.findByEmployeeIdAndLeaveTypeIsNotNull(employeeId);

        List<LeaveBalanceDTO> balances = new ArrayList<>();
        balances.add(buildBalance("ANNUAL_LEAVE", 12.0, allLeaves));
        balances.add(buildBalance("SICK_LEAVE", 10.0, allLeaves));
        balances.add(buildBalance("PERSONAL_LEAVE", 5.0, allLeaves));

        return balances;
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
        double requestedDays = ChronoUnit.DAYS.between(dto.getLeaveFrom(), dto.getLeaveTo()) + 1;
        List<LeaveBalanceDTO> balances = getLeaveBalances(employeeId);

        LeaveBalanceDTO balance = balances.stream()
                .filter(b -> b.getLeaveType().equals(dto.getLeaveType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported leave type."));

        if (!balance.isRequestable()) {
            throw new RuntimeException("This leave type has no remaining days.");
        }

        if (requestedDays > balance.getRemainingDays()) {
            throw new RuntimeException("Insufficient leave balance. You have "
                    + (int) balance.getRemainingDays() + " day(s) remaining.");
        }

        // ── Map leaveType -> request_type code ────────────
        String requestTypeCode = switch (dto.getLeaveType()) {
            case "SICK_LEAVE" -> "LEAVE_SICK";
            case "PERSONAL_LEAVE" -> "LEAVE_UNPAID";
            default -> "LEAVE_ANNUAL";
        };

        RequestType requestType = requestTypeRepository.findByCode(requestTypeCode)
                .orElseThrow(() -> new RuntimeException("Request type not found: " + requestTypeCode));

        // ── Tạo request ────────────────────────────────────
        Request request = new Request();
        request.setEmployeeId(employeeId);
        request.setRequestTypeId(requestType.getId());
        request.setLeaveType(dto.getLeaveType());
        request.setLeaveFrom(dto.getLeaveFrom());
        request.setLeaveTo(dto.getLeaveTo());
        request.setContent(dto.getContent());
        request.setTitle(dto.getLeaveType().replace("_", " ") + " Request");
        request.setStatus("PENDING");
        request.setStep(WorkflowConstants.STEP_WAITING_DM);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        Request savedRequest = requestRepository.save(request);
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
        double used = allLeaves.stream()
                .filter(r -> leaveType.equals(r.getLeaveType()))
                .filter(r -> "APPROVED".equals(r.getStatus()))
                .filter(r -> r.getLeaveFrom() != null && r.getLeaveTo() != null)
                .mapToDouble(r -> ChronoUnit.DAYS.between(r.getLeaveFrom(), r.getLeaveTo()) + 1)
                .sum();

        double remaining = Math.max(0, total - used);
        double usagePercentage = total > 0 ? Math.round((used / total) * 100) : 0;

        return LeaveBalanceDTO.builder()
                .leaveType(leaveType)
                .totalDays(total)
                .usedDays(used)
                .remainingDays(remaining)
                .usagePercentage(usagePercentage)
                .requestable(remaining > 0)
                .build();
    }

    private LeaveRequestDTO mapToDTO(Request req) {
        return LeaveRequestDTO.builder()
                .id(req.getId())
                .leaveType(req.getLeaveType())
                .leaveFrom(req.getLeaveFrom())
                .leaveTo(req.getLeaveTo())
                .content(req.getContent())
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
}
