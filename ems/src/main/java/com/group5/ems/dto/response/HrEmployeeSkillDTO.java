package com.group5.ems.dto.response;

import lombok.Builder;

@Builder
public record HrEmployeeSkillDTO(
        Long skillId,
        String name,
        String category,
        Integer proficiency
) {
}
