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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HRManagerDashboardService {
    
    private final EmployeeRepository employeeRepository;
    private final RequestRepository requestRepository;
    private final JobPostRepository jobPostRepository;
    
    public DashboardKpiResponseDTO getKpiData() {
        DashboardKpiResponseDTO kpi = new DashboardKpiResponseDTO();
        
        // 1. Total Headcount - Tổng số nhân viên đang active
        Long totalHeadcount = employeeRepository.countByStatus("ACTIVE");
        kpi.setTotalHeadcount(totalHeadcount.intValue());
        
        // Giả sử tăng 2.5% so với tháng trước (có thể tính toán thực tế sau)
        kpi.setHeadcountChange("+2.5%");
        kpi.setHeadcountChangePositive(true);
        
        // 2. Open Requisitions - Số job posts đang mở
        List<JobPost> openJobs = jobPostRepository.findByStatus("OPEN");
        kpi.setOpenRequisitions(openJobs.size());
        
        // Tính % thay đổi (giả sử giảm 10%)
        kpi.setOpenReqChange("-10%");
        kpi.setOpenReqChangePositive(false);
        
        // 3. Monthly Turnover - Tính từ số nhân viên terminated trong tháng
        // Tạm thời dùng giá trị mẫu, có thể tính toán thực tế sau
        kpi.setMonthlyTurnover("1.2%");
        kpi.setTurnoverChange("+0.1%");
        kpi.setTurnoverChangePositive(false); // Tăng turnover là không tốt
        
        // 4. Average Tenure - Tính thời gian làm việc trung bình
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
        // Tạm thời dùng dữ liệu mẫu, có thể tích hợp với calendar system sau
        return Arrays.asList(
            new UpcomingEventDTO("New Employee Orientation", "OCT", "12", "09:00 AM - 11:00 AM", "blue"),
            new UpcomingEventDTO("Q3 Performance Reviews", "OCT", "14", "All Day", "purple"),
            new UpcomingEventDTO("Benefits Enrollment Ends", "OCT", "15", "05:00 PM Deadline", "emerald")
        );
    }
    
    public List<RecentActivityDTO> getRecentActivities() {
        // Lấy các request gần đây nhất (pending hoặc mới approved trong 7 ngày qua)
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<Request> recentRequests = requestRepository.findRecentActivities(sevenDaysAgo.atStartOfDay());
        
        return recentRequests.stream()
            .limit(10) // Giới hạn 10 items
            .map(this::mapToRecentActivityDTO)
            .collect(Collectors.toList());
    }
    
    private RecentActivityDTO mapToRecentActivityDTO(Request request) {
        // Lấy thông tin employee
        Employee employee = employeeRepository.findById(request.getEmployeeId()).orElse(null);
        
        String employeeName = "Unknown";
        String employeePosition = "N/A";
        String employeeInitials = "N/A";
        
        if (employee != null && employee.getUser() != null) {
            employeeName = employee.getUser().getFullName();
            employeeInitials = getInitials(employeeName);
            // Có thể lấy position từ employee.getPosition() nếu có relationship
            employeePosition = "Employee"; // Tạm thời
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