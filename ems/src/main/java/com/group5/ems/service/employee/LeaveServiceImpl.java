package com.group5.ems.service.employee;

import com.group5.ems.dto.request.CreateLeaveRequestDTO;
import com.group5.ems.dto.response.LeaveBalanceDTO;
import com.group5.ems.dto.response.LeaveRequestDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.RequestType;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.RequestTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl {

    private final EmployeeRepository employeeRepository;
    private final RequestRepository requestRepository;
    private final RequestTypeRepository requestTypeRepository;

    @Transactional(readOnly = true)
    public List<LeaveBalanceDTO> getLeaveBalances(Long employeeId) {
        List<Request> allLeaves = requestRepository.findByEmployeeIdAndLeaveTypeIsNotNull(employeeId);

        // Tính balance cho từng loại leave
        List<LeaveBalanceDTO> balances = new ArrayList<>();
        balances.add(buildBalance("ANNUAL_LEAVE", "LEAVE_ANNUAL", 12.0, allLeaves));
        balances.add(buildBalance("SICK_LEAVE", "LEAVE_SICK", 10.0, allLeaves));
        balances.add(buildBalance("PERSONAL_LEAVE", "LEAVE_UNPAID", 5.0, allLeaves));

        return balances;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDTO> getLeaveHistory(Long employeeId) {
        return requestRepository.findByEmployeeIdAndLeaveTypeIsNotNull(employeeId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createLeaveRequest(Long employeeId, CreateLeaveRequestDTO dto) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Map leaveType -> request_type code
        String requestTypeCode = switch (dto.getLeaveType()) {
            case "SICK_LEAVE" -> "LEAVE_SICK";
            case "PERSONAL_LEAVE" -> "LEAVE_UNPAID";
            default -> "LEAVE_ANNUAL";
        };

        RequestType requestType = requestTypeRepository.findByCode(requestTypeCode)
                .orElseThrow(() -> new RuntimeException("Request type not found: " + requestTypeCode));

        Request request = new Request();
        request.setEmployeeId(employeeId);
        request.setRequestTypeId(requestType.getId());
        if (dto.getLeaveFrom() != null)
            request.setStartDate(dto.getLeaveFrom().atStartOfDay());
        if (dto.getLeaveTo() != null)
            request.setEndDate(dto.getLeaveTo().atStartOfDay());
        request.setContent(dto.getContent());
        request.setTitle(dto.getLeaveType().replace("_", " ") + " request");
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        requestRepository.save(request);
    }

    // ── Helper methods ──────────────────────────────────────

    private LeaveBalanceDTO buildBalance(String leaveType, String expectedCode, double total, List<Request> allLeaves) {
        double used = allLeaves.stream()
                .filter(r -> r.getRequestType() != null && expectedCode.equals(r.getRequestType().getCode()))
                .filter(r -> "APPROVED".equals(r.getStatus()))
                .filter(r -> r.getStartDate() != null && r.getEndDate() != null)
                .mapToDouble(
                        r -> ChronoUnit.DAYS.between(r.getStartDate().toLocalDate(), r.getEndDate().toLocalDate()) + 1)
                .sum();

        double remaining = Math.max(0, total - used);
        double usagePercentage = total > 0 ? Math.round((used / total) * 100) : 0;

        return LeaveBalanceDTO.builder()
                .leaveType(leaveType)
                .totalDays(total)
                .usedDays(used)
                .remainingDays(remaining)
                .usagePercentage(usagePercentage)
                .build();
    }

    private LeaveRequestDTO mapToDTO(Request req) {
        String leaveType = switch (req.getRequestType() != null ? req.getRequestType().getCode() : "") {
            case "LEAVE_SICK" -> "SICK_LEAVE";
            case "LEAVE_UNPAID" -> "PERSONAL_LEAVE";
            default -> "ANNUAL_LEAVE";
        };

        return LeaveRequestDTO.builder()
                .id(req.getId())
                .leaveType(leaveType)
                .leaveFrom(req.getStartDate() != null ? req.getStartDate().toLocalDate() : null)
                .leaveTo(req.getEndDate() != null ? req.getEndDate().toLocalDate() : null)
                .content(req.getContent())
                .status(req.getStatus())
                .rejectedReason(req.getRejectedReason())
                .build();
    }
}