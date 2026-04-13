package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.group5.ems.util.WorkingDayUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestDTO {
    private Long id;
    private String leaveType;   // ANNUAL_LEAVE, SICK_LEAVE, UNPAID_LEAVE
    private LocalDate leaveFrom;
    private LocalDate leaveTo;
    private String content;
    private String status;      // PENDING, APPROVED, REJECTED
    private String rejectedReason;
    private String step;
    private String statusDisplay;
    private String stepDisplay;
    private LocalDateTime createdAt;
    private boolean cancelable;
    private boolean urgent;

    public long getDaysCount() {
        if (leaveFrom == null || leaveTo == null) return 0;
        return WorkingDayUtils.countWorkingDays(leaveFrom, leaveTo);
    }
}
