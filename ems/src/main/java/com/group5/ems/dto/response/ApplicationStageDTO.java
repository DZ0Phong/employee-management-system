package com.group5.ems.dto.response;

public record ApplicationStageDTO(
                Long id,
                String stageName,
                String note,
                String changedAtFormatted,
                String changedBy) {
}