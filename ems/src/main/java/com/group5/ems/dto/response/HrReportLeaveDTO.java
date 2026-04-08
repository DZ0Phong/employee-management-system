package com.group5.ems.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record HrReportLeaveDTO(
        long totalApproved,
        long totalRejected,
        long totalPending,
        Double avgProcessingHours,
        List<String> leaveTypeLabels,
        List<Long> leaveTypeCounts,
        List<String> monthlyLabels,
        List<Long> monthlyApproved,
        List<Long> monthlyRejected
) {}
