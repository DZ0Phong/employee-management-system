package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSummaryDTO {
    private int pendingCount;
    private String pendingChangeLabel;
    private boolean pendingChangePositive;

    private String totalValueFormatted;   // "€450,230"
    private int employeesCovered;
}