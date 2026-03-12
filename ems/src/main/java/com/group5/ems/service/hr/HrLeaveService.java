package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrLeaveRequestDTO;
import com.group5.ems.entity.Request;
import com.group5.ems.repository.RequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HrLeaveService {

    private final RequestRepository requestRepository;

    public HrLeaveService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Pending leaves are always shown in full (small count). Uses DB-level JOIN FETCH.
     */
    public List<HrLeaveRequestDTO> getPendingLeaves() {
        return requestRepository.findPendingLeaveRequests().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Leave history is paginated at DB level — only fetches one page at a time.
     */
    public Page<HrLeaveRequestDTO> getLeaveHistory(Pageable pageable) {
        Page<Request> page = requestRepository.findLeaveHistory(pageable);
        List<HrLeaveRequestDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional
    public void approveLeave(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        request.setStatus("APPROVED");
        request.setApprovedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    @Transactional
    public void rejectLeave(Long id, String reason) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        request.setStatus("REJECTED");
        request.setRejectedReason(reason);
        requestRepository.save(request);
    }

    private HrLeaveRequestDTO mapToDTO(Request request) {
        String initials = "";
        String fullName = "Unknown";
        String departmentName = "N/A";
        String employeeCode = "N/A";
        
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
            }
            if (request.getEmployee().getEmployeeCode() != null) {
                employeeCode = request.getEmployee().getEmployeeCode();
            }
        }

        String duration = "N/A";
        String dates = "N/A";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
            long days = ChronoUnit.DAYS.between(request.getLeaveFrom(), request.getLeaveTo()) + 1;
            duration = days + (days == 1 ? " Day" : " Days");
            dates = request.getLeaveFrom().format(dtf) + " - " + request.getLeaveTo().format(dtf);
        } else if (request.getLeaveFrom() != null) {
            duration = "1 Day";
            dates = request.getLeaveFrom().format(dtf);
        }

        String reason = request.getContent() != null ? request.getContent() : "No reason provided";

        return HrLeaveRequestDTO.builder()
                .id(request.getId())
                .employeeName(fullName)
                .initials(initials.toUpperCase())
                .department(departmentName)
                .employeeCode(employeeCode)
                .leaveType(request.getRequestType() != null ? request.getRequestType().getName() : "N/A")
                .duration(duration)
                .dates(dates)
                .reason(reason)
                .leave_from(request.getLeaveFrom())
                .leave_to(request.getLeaveTo())
                .status(request.getStatus())
                .build();
    }
}
