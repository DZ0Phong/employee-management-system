package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceSummaryDTO {
    private BigDecimal currentRating;       // điểm review mới nhất
    private BigDecimal previousRating;      // điểm review trước đó
    private int totalReviews;
}
