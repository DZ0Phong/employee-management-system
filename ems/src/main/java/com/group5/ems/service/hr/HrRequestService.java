package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrRequestDTO;
import com.group5.ems.entity.Request;
import com.group5.ems.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrRequestService {

    private final RequestRepository requestRepository;

    public Page<HrRequestDTO> getAllWorkflowRequests(Pageable pageable) {
        Page<Request> page = requestRepository.findWorkflowRequests(pageable);
        List<HrRequestDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional
    public void approveRequest(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus("APPROVED");
        request.setApprovedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    @Transactional
    public void rejectRequest(Long id, String reason) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus("REJECTED");
        request.setRejectedReason(reason);
        requestRepository.save(request);
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