package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.repository.ApplicationRepository;
import com.group5.ems.repository.AttendanceRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrDashboardService {

    private final EmployeeRepository employeeRepository;
    private final JobPostRepository jobPostRepository;
    private final RequestRepository requestRepository;
    private final AttendanceRepository attendanceRepository;
    private final ApplicationRepository applicationRepository;

    public HrDashboardMetricsDTO getDashboardMetrics() {
        Long activeEmployees = employeeRepository.countByStatus("ACTIVE");
        int openJobPosts = jobPostRepository.countByStatus("OPEN");
        long pendingLeaveRequestsLong = requestRepository.countByStatusAndStepWaitingHRAndRequestTypeCodeIn(
                "PENDING", 
                java.util.Arrays.asList("LV_ANNUAL", "LV_SICK", "LEAVE_ANNUAL", "LEAVE_SICK", "LEAVE_UNPAID")
        );
        int pendingLeaveRequests = (int) pendingLeaveRequestsLong;
        int pendingWorkflowRequests = (int) requestRepository.countByStatusAndStepWaitingHRAndRequestTypeCategory("PENDING", "HR_STATUS");
        long newHiresThisMonth = employeeRepository.newThisMonth();
        int totalApplicants = (int) applicationRepository.count();

        // Attendance last 7 days
        List<String> attendanceLabels = new ArrayList<>();
        List<Integer> attendancePresent = new ArrayList<>();
        List<Integer> attendanceLeave = new ArrayList<>();
        List<Integer> attendanceAbsent = new ArrayList<>();
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("EEE M/d");

        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            attendanceLabels.add(day.format(labelFmt));

            int present = attendanceRepository.countByWorkDateAndStatus(day, "PRESENT");
            int onLeave = attendanceRepository.countByWorkDateAndStatus(day, "LATE");
            int totalForDay = attendanceRepository.countByWorkDate(day);
            int absent = Math.max(0, totalForDay - present - onLeave);

            attendancePresent.add(present);
            attendanceLeave.add(onLeave);
            attendanceAbsent.add(absent);
        }

        // Recruitment pipeline counts
        int pipelineApplied = applicationRepository.countByStatus("APPLIED");
        int pipelineReviewing = applicationRepository.countByStatus("SCREENING");
        int pipelineInterviewing = applicationRepository.countByStatus("INTERVIEWING");
        int pipelineOfferSent = applicationRepository.countByStatus("OFFER");

        return HrDashboardMetricsDTO.builder()
                .activeEmployees(activeEmployees)
                .pendingLeaveRequests(pendingLeaveRequests)
                .openJobPosts(openJobPosts)
                .pendingWorkflowRequests(pendingWorkflowRequests)
                .newHiresThisMonth(newHiresThisMonth)
                .totalApplicants(totalApplicants)
                .attendanceLabels(attendanceLabels)
                .attendancePresent(attendancePresent)
                .attendanceLeave(attendanceLeave)
                .attendanceAbsent(attendanceAbsent)
                .pipelineApplied(pipelineApplied)
                .pipelineReviewing(pipelineReviewing)
                .pipelineInterviewing(pipelineInterviewing)
                .pipelineOfferSent(pipelineOfferSent)
                .build();
    }

    public Long findEmployeeIdByCode(String code) {
        return employeeRepository.findByEmployeeCode(code)
                .map(com.group5.ems.entity.Employee::getId)
                .orElse(null);
    }
}