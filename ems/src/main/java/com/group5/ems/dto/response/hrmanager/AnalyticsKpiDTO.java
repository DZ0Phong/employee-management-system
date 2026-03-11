package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AnalyticsKpiDTO {
    private int totalWorkforce;
    private String workforceChange;
    private boolean workforceChangePositive;

    private String retentionRate;
    private String retentionChange;
    private boolean retentionChangePositive;

    private int openPositions;
    private String hiringVelocity;
    private boolean hiringVelocityPositive;

    private String averageSalary;
    private String salaryChange;
    private boolean salaryChangePositive;
}
