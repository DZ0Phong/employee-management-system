package com.group5.ems.dto.response;

import lombok.Builder;


/**
 * Aggregated leave statistics for the HR leave management dashboard.
 */
@Builder
public record HrLeaveStatsDTO(
        long totalPending,
        long approvedThisMonth,
        long rejectedThisMonth,
        long onLeaveToday,
        double avgProcessingHours,
        String topLeaveType,
        long topLeaveTypeCount
) {
}
