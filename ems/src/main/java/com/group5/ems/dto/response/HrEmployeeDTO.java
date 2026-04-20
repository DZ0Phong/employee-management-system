package com.group5.ems.dto.response;

import lombok.Builder;

@Builder
public record HrEmployeeDTO(
        Long id,
        String initials,
        String fullName,
        String position,
        String department,
        String code,
        String status,
        String email,
        String phone,
        String avatarUrl
) {
}