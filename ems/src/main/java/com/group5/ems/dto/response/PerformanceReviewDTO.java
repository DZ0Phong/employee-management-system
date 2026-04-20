package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceReviewDTO {
    private Long id;
    private String reviewPeriod;
    private String reviewerName;
    private BigDecimal performanceScore;
    private BigDecimal potentialScore;
    private String strengths;
    private String areasToImprove;
    private String status;
    private String statusDisplay;
    private String reviewPeriodDisplay;
    private LocalDateTime createdAt;
}
