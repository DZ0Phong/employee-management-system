package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponseDTO {

    private Long applicationId;
    private Long candidateId;
    private Long jobId;
    private Long cvId;
    private String trackingToken;

    // ── Thêm các field này ──
    private String status;
    private String appliedAt;
    private JobPostInfo jobPost;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobPostInfo {
        private String title;
    }
}