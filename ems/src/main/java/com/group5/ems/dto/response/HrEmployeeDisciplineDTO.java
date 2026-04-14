package com.group5.ems.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record HrEmployeeDisciplineDTO(
        Long id,
        String recordType,
        String title,
        String description,
        LocalDate decisionDate,
        BigDecimal amount,
        String decidedBy
) {
}
