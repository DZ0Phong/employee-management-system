package com.group5.ems.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record HrReportPayrollDTO(
        Double avgSalary,
        BigDecimal totalPayrollCost,
        List<String> salaryBandLabels,
        List<Long> salaryBandCounts,
        List<String> deptAvgLabels,
        List<Double> deptAvgValues
) {}
