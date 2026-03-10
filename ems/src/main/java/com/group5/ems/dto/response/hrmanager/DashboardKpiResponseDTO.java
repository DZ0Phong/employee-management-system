package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiResponseDTO {
    private Integer totalHeadcount;
    private String headcountChange;
    private Boolean headcountChangePositive;
    
    private Integer openRequisitions;
    private String openReqChange;
    private Boolean openReqChangePositive;
    
    private String monthlyTurnover;
    private String turnoverChange;
    private Boolean turnoverChangePositive;
    
    private String averageTenure;
    private String tenureChange;
    private Boolean tenureChangePositive;
}