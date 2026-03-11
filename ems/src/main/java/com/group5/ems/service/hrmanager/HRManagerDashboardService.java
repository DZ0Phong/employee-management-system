package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.DashboardKpiResponseDTO;
import com.group5.ems.dto.response.hrmanager.RecentActivityDTO;
import com.group5.ems.dto.response.hrmanager.UpcomingEventDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.JobPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HRManagerDashboardService {
    
    private final EmployeeRepository employeeRepository;
    private final RequestRepository requestRepository;
    private final JobPostRepository jobPostRepository;
    
    public DashboardKpiResponseDTO getKpiData() {
        DashboardKpiResponseDTO kpi = new DashboardKpiResponseDTO();
        
        // 1. Total Headcount - Tổng số nhân viên đang active
        Long totalHeadcount = employeeRepository.countByStatus("ACTIVE");
        kpi.setTotalHeadcount(totalHeadcount.intValue());
        kpi.setHeadcountChange("+2.5%");
        kpi.setHeadcountChangePositive(true);
        
        // 2. Open Requisitions - Số job posts đang mở
        List<JobPost> openJobs = jobPostRepository.findByStatus("OPEN");
        kpi.setOpenRequisitions(openJobs.size());
        kpi.setOpenReqChange("-10%");
        kpi.setOpenReqChangePositive(false);
        
        // 3. Monthly Turnover
        kpi.setMonthlyTurnover("1.2%");
        kpi.setTurnoverChange("+0.1%");
        kpi.setTurnoverChangePositive(false);
        
        // 4. Average Tenure
        Double avgTenureDays = employeeRepository.getAverageTenureInDays();
        if (avgTenureDays != null) {
            double avgTenureYears = avgTenureDays / 365.0;
            kpi.setAverageTenure(String.format("%.1f Yrs", avgTenureYears));
        } else {
            kpi.setAverageTenure("0.0 Yrs");
        }
        kpi.setTenureChange("-0.2%");
        kpi.setTenureChangePositive(false);
        
        return kpi;
    }
    
    public List<String> getChartMonths() {
        return Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun");
    }
    
    public List<UpcomingEventDTO> getUpcomingEvents() {
        return Arrays.asList(
            new UpcomingEventDTO("New Employee Orientation", "OCT", "12", "09:00 AM - 11:00 AM", "blue"),
            new UpcomingEventDTO("Q3 Performance Reviews", "OCT", "14", "All Day", "purple"),
            new UpcomingEventDTO("Benefits Enrollment Ends", "OCT", "15", "05:00 PM Deadline", "emerald")
        );
    }
    
    public List<RecentActivityDTO> getRecentActivities() {
        // Lấy các request gần đây nhất
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<Request> recentRequests = requestRepository.findRecentActivities(sevenDaysAgo.atStartOfDay());
        
        return recentRequests.stream()
            .limit(10)
            .map(this::mapToRecentActivityDTO)
            .collect(Collectors.toList());
    }
    
    private RecentActivityDTO mapToRecentActivityDTO(Request request) {
        Employee employee = request.getEmployee();
        
        String employeeName = "Unknown";
        String employeePosition = "N/A";
        String employeeInitials = "N/A";
        
        if (employee != null) {
            if (employee.getUser() != null) {
                employeeName = employee.getUser().getFullName();
                employeeInitials = getInitials(employeeName);
            }
            
            if (employee.getPosition() != null) {
                employeePosition = employee.getPosition().getName();
            } else {
                employeePosition = "Employee";
            }
        }
        
        return new RecentActivityDTO(
            request.getId(),
            employeeName,
            employeePosition,
            employeeInitials,
            request.getTitle() != null ? request.getTitle() : "Request",
            request.getCreatedAt().toLocalDate(),
            request.getStatus(),
            request.getStatus()
        );
    }
    
    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "N/A";
        
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(part.charAt(0));
            }
        }
        
        return initials.toString().toUpperCase();
    }
}