package com.group5.ems.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record HrRequestDTO(
    Long id,
    String requestedBy,
    String department,
    String category,
    String title,
    String content,
    String status,
    LocalDateTime submittedAt
) {
}
