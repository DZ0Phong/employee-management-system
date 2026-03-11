package com.group5.ems.dto.response;

import lombok.Builder;

@Builder
public record HrRecruitmentDTO(
    Long id,
    String jobTitle,
    String department,
    String location,
    String status,
    int applicantCount
) {
}
