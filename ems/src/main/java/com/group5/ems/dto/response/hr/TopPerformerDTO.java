package com.group5.ems.dto.response.hr;

import lombok.Builder;

@Builder
public record TopPerformerDTO(
        Long employeeId,
        Double averageScore
) {
}
