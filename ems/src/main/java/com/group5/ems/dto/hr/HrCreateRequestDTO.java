package com.group5.ems.dto.hr;

import lombok.Builder;

/**
 * Form DTO for HR creating a new workflow request (as themselves).
 */
@Builder
public record HrCreateRequestDTO(
    Long requestTypeId,
    String title,
    String content
) {
}
