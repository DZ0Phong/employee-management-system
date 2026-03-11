package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveStatsDTO {
    private int pendingCount;
    private String pendingChange;

    private int approvedTodayCount;
    private int completionRate;

    private int onLeaveNowCount;
    private String nextReturnInfo;
}