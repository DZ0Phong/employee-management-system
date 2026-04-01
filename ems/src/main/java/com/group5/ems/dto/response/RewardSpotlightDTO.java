package com.group5.ems.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RewardSpotlightDTO {
    private String employeeName;
    private String initials;
    private String departmentName;
    private String awardTitle;
    private BigDecimal amount;
    private LocalDate decisionDate;
}