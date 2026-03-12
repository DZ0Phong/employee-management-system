package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.RequestRepository;
import org.springframework.stereotype.Service;

@Service
public class HrDashboardService {

    private final EmployeeRepository employeeRepository;
    private final JobPostRepository jobPostRepository;
    private final RequestRepository requestRepository;

    public HrDashboardService(EmployeeRepository employeeRepository, JobPostRepository jobPostRepository, RequestRepository requestRepository) {
        this.employeeRepository = employeeRepository;
        this.jobPostRepository = jobPostRepository;
        this.requestRepository = requestRepository;
    }

    public HrDashboardMetricsDTO getDashboardMetrics() {
        Long activeEmployees = employeeRepository.countByStatus("ACTIVE");
        int openJobPosts = jobPostRepository.countByStatus("OPEN");
        // category ATTENDANCE is for Leave
        int pendingLeaveRequests = requestRepository.countByStatusAndRequestTypeCategory("PENDING", "ATTENDANCE");
        // category HR_STATUS is for Workflow Tasks and Recruitment
        int pendingWorkflowRequests = requestRepository.countByStatusAndRequestTypeCategory("PENDING", "HR_STATUS");

        return HrDashboardMetricsDTO.builder()
                .activeEmployees(activeEmployees)
                .pendingLeaveRequests(pendingLeaveRequests)
                .openJobPosts(openJobPosts)
                .pendingWorkflowRequests(pendingWorkflowRequests)
                .build();
    }
}
