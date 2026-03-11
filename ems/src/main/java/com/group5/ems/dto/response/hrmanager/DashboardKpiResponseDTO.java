package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiResponseDTO {

    // Tổng nhân viên
    private int totalHeadcount;
    private String headcountChange;
    private boolean headcountChangePositive;

    // Job đang mở
    private int openRequisitions;
    private String openReqChange;
    private boolean openReqChangePositive;

    // Turnover tháng
    private String monthlyTurnover;
    private String turnoverChange;
    private boolean turnoverChangePositive;

    // Thâm niên trung bình
    private String averageTenure;
    private String tenureChange;
    private boolean tenureChangePositive;
}