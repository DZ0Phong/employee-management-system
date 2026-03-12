package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrLeaveRequestDTO;
import com.group5.ems.entity.Request;
import com.group5.ems.repository.RequestRepository;
import org.springframework.stereotype.Service;

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

    public List<HrLeaveRequestDTO> getPendingLeaves() {
        return requestRepository.findByStatus("PENDING").stream()
                .filter(r -> "ATTENDANCE".equals(r.getRequestType().getCategory()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<HrLeaveRequestDTO> getLeaveHistory() {
        return requestRepository.findAll().stream()
                .filter(r -> "ATTENDANCE".equals(r.getRequestType().getCategory()) && !"PENDING".equals(r.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private HrLeaveRequestDTO mapToDTO(Request request) {
        String initials = "";
        String fullName = "Unknown";
        String departmentName = "N/A";
        
        if (request.getEmployee() != null && request.getEmployee().getUser() != null) {
            fullName = request.getEmployee().getUser().getFullName();
            if (fullName != null && !fullName.trim().isEmpty()) {
                String[] names = fullName.trim().split("\\s+");
                initials += names[0].charAt(0);
                if (names.length > 1) {
                    initials += names[names.length - 1].charAt(0);
                }
            }
            if (request.getEmployee().getDepartment() != null) {
                departmentName = request.getEmployee().getDepartment().getName();
            }
        }

        String duration = "N/A";
        if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
            long days = ChronoUnit.DAYS.between(request.getLeaveFrom(), request.getLeaveTo()) + 1;
            duration = days + (days == 1 ? " Day" : " Days");
        }

        return HrLeaveRequestDTO.builder()
                .id(request.getId())
                .employeeName(fullName)
                .initials(initials.toUpperCase())
                .department(departmentName)
                .leaveType(request.getRequestType().getName())
                .duration(duration)
                .startDate(request.getLeaveFrom())
                .endDate(request.getLeaveTo())
                .status(request.getStatus())
                .build();
    }
}
