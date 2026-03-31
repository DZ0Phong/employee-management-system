package com.group5.ems.dto.response;

import lombok.Builder;

@Builder
public record HrRequestStatsDTO(
    long totalPending,
    long approvedThisMonth,
    long rejectedThisMonth,
    double avgProcessingHours,
    String topRequestType,
    long topRequestTypeCount
) {
}
