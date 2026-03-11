package com.group5.ems.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record HrApplicantDTO(
    Long id,
    String applicantName,
    String email,
    String appliedJob,
    LocalDateTime appliedDate,
    String stage
) {
}
