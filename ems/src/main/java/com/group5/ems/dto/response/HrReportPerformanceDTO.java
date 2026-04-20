package com.group5.ems.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
public record HrReportPerformanceDTO(
        Double avgPerformanceScore,
        Double avgPotentialScore,
        long totalReviews,
        Map<String, Long> performanceGradeDistribution,
        List<String> scoreLabels,
        List<Long> scoreCounts,
        List<TopPerformer> topPerformers
) {
    @Builder
    public record TopPerformer(String name, String department, BigDecimal score) {}
}
