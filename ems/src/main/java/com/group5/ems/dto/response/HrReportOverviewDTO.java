package com.group5.ems.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record HrReportOverviewDTO(
        long totalHeadcount,
        long activeCount,
        long terminatedCount,
        long onLeaveCount,
        Double avgTenureDays,
        long newHiresThisMonth,
        long terminationsThisMonth,
        Double turnoverRate,
        List<DeptCount> departmentBreakdown,
        List<String> monthlyLabels,
        List<Long> monthlyHires,
        List<Long> monthlyTerminations
) {
    @Builder
    public record DeptCount(String name, long count) {}
}
