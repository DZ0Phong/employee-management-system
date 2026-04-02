package com.group5.ems.dto.response;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record HrEmployeeLeaveBalanceDTO(
    Long employeeId,
    String employeeName,
    String employeeCode,
    String departmentName,
    Integer year,
    BigDecimal totalDays,
    BigDecimal usedDays,
    BigDecimal pendingDays,
    BigDecimal remainingDays
) {}
