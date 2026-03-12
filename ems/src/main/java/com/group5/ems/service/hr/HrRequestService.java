package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrRequestDTO;
import com.group5.ems.entity.Request;
import com.group5.ems.repository.RequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HrRequestService {

    private final RequestRepository requestRepository;

    public HrRequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public List<HrRequestDTO> getAllWorkflowRequests() {
        return requestRepository.findAll().stream()
                .filter(req -> req.getRequestType() != null && "HR_STATUS".equals(req.getRequestType().getCategory()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private HrRequestDTO mapToDTO(Request request) {
        String empName = "Unknown";
        String deptName = "N/A";
        String category = "N/A";

        if (request.getEmployee() != null) {
            if (request.getEmployee().getUser() != null) {
                empName = request.getEmployee().getUser().getFullName();
            }
            if (request.getEmployee().getDepartment() != null) {
                deptName = request.getEmployee().getDepartment().getName();
            }
        }
        
        if (request.getRequestType() != null) {
            category = request.getRequestType().getName();
        }

        return HrRequestDTO.builder()
                .id(request.getId())
                .requestedBy(empName)
                .department(deptName)
                .category(category)
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus())
                .submittedAt(request.getCreatedAt())
                .build();
    }
}
