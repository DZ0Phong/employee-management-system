package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceStatsDTO {
    private String totalHours;      // VD: "164h 20m"
    private double onTimeRate;      // VD: 98.5
    private int presentDays;        // VD: 21
    private int totalWorkDays;      // VD: 22
    private boolean clockedInToday;
    private boolean clockedOutToday;
}