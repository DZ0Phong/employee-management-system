package com.group5.ems.dto.response;

import com.group5.ems.dto.response.hr.HrEventDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record HrDashboardMetricsDTO(
        Long activeEmployees,
        int pendingLeaveRequests,
        int openJobPosts,
        int pendingWorkflowRequests,
        long newHiresThisMonth,
        int totalApplicants,
        List<String> attendanceLabels,
        List<Integer> attendancePresent,
        List<Integer> attendanceLeave,
        List<Integer> attendanceAbsent,
        int pipelineApplied,
        int pipelineReviewing,
        int pipelineInterviewing,
        int pipelineOfferSent,
        List<HrEventDTO> upcomingEvents
) {
}