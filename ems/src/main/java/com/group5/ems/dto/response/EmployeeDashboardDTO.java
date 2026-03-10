package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDashboardDTO {
    private Double leaveBalance;
    private Double attendanceRate;
    private String attendanceTrend;
    private Double lastPayroll;
    private Double performanceRating;
}