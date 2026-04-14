package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceDTO {
    private String leaveType;       // ANNUAL_LEAVE, SICK_LEAVE, UNPAID_LEAVE
    private double totalDays;
    private double usedDays;
    private double pendingDays;
    private double remainingDays;
    private double usagePercentage;
    private boolean requestable;
    private boolean unlimited;
}
