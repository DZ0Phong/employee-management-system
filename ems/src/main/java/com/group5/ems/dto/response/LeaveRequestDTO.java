package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestDTO {
    private Long id;
    private String leaveType;   // ANNUAL_LEAVE, SICK_LEAVE, PERSONAL_LEAVE
    private LocalDate leaveFrom;
    private LocalDate leaveTo;
    private String content;
    private String status;      // PENDING, APPROVED, REJECTED
    private String rejectedReason;

    public long getDaysCount() {
        if (leaveFrom == null || leaveTo == null) return 0;
        return ChronoUnit.DAYS.between(leaveFrom, leaveTo) + 1;
    }
}