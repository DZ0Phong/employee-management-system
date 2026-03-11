package com.group5.ems.dto.response;

import lombok.Builder;

@Builder
public record HrDashboardMetricsDTO(
    Long activeEmployees,
    int pendingLeaveRequests,
    int openJobPosts,
    int pendingWorkflowRequests
) {
}
