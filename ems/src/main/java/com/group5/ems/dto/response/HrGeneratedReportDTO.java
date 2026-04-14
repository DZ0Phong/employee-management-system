package com.group5.ems.dto.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record HrGeneratedReportDTO(
        Long id,
        String title,
        String reportType,
        String format,
        String status,
        String remarks,
        boolean isPublished,
        LocalDateTime generatedAt,
        LocalDateTime publishedAt,
        String generatedByName
) {}
