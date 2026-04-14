package com.group5.ems.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaveRequestDTO {
    private String leaveType;   // ANNUAL_LEAVE, SICK_LEAVE, UNPAID_LEAVE

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate leaveFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate leaveTo;

    private String content;
    private boolean urgent;
}
