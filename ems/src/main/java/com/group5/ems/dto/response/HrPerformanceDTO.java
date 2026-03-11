package com.group5.ems.dto.response;

import lombok.Builder;

@Builder
public record HrPerformanceDTO(
    Long id,
    String employeeName,
    String department,
    String reviewerName,
    String status,
    String score
) {
}
