package com.group5.ems.dto.response;

public record CandidateCvDTO(
                Long id,
                String fileName,
                String fileType,
                String uploadedAtFormatted) {
}