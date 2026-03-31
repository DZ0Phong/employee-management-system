package com.group5.ems.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record HrRequestDTO(
    Long id,
    String requestedBy,
    String initials,
    String department,
    Long departmentId,
    String employeeCode,
    String category,
    String categoryCode,
    String title,
    String content,
    String status,
    String rejectedReason,
    LocalDateTime submittedAt,
    String processedAt,
    String approverName
) {
}
