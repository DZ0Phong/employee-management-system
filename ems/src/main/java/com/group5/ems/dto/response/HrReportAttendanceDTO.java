package com.group5.ems.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record HrReportAttendanceDTO(
        double presentRate,
        double lateRate,
        double absentRate,
        long totalRecords,
        long presentCount,
        long lateCount,
        long absentCount,
        List<String> dailyLabels,
        List<Long> dailyPresent,
        List<Long> dailyLate,
        List<Long> dailyAbsent
) {}
