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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HRManagerDashboardService {
    
    private final EmployeeRepository employeeRepository;
    private final RequestRepository requestRepository;
    private final JobPostRepository jobPostRepository;
    private final CalendarService calendarService; // ← thêm
    
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
        List<String> months = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM");

        for (int i = 5; i >= 0; i--) {
            months.add(now.minusMonths(i).format(fmt));
        }
        return months; // ví dụ: ["Oct", "Nov", "Dec", "Jan", "Feb", "Mar"]
    }

    public List<UpcomingEventDTO> getUpcomingEvents() {
        return calendarService.getUpcomingEvents()
                .stream()
                .map(e -> new UpcomingEventDTO(
                        e.getTitle(),
                        e.getMonthLabel(),
                        e.getDayLabel(),
                        e.getTimeLabel(),
                        e.getColor()
                ))
                .collect(Collectors.toList());
    }

    public List<RecentActivityDTO> getRecentActivities(String filter) {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<Request> requests;

        if ("pending".equals(filter)) {
            requests = requestRepository.findByStatus("PENDING");
        } else if ("approved".equals(filter)) {
            requests = requestRepository.findByStatus("APPROVED");
        } else {
            requests = requestRepository.findRecentActivities(sevenDaysAgo.atStartOfDay());
        }

        return requests.stream()
                .limit(10)
                .map(this::mapToRecentActivityDTO)
                .collect(Collectors.toList());
    }

    public List<Integer> getHiringData() {
        int currentYear = LocalDate.now().getYear();
        List<Object[]> rawData = employeeRepository.countHiringByMonth(currentYear);

        // Tạo mảng 6 tháng gần nhất, mặc định = 0
        int currentMonth = LocalDate.now().getMonthValue();
        int[] months = new int[6];
        for (int i = 0; i < 6; i++) {
            months[i] = currentMonth - 5 + i; // tháng từ 6 tháng trước đến hiện tại
            if (months[i] <= 0) months[i] += 12; // xử lý năm trước
        }

        // Map data từ DB vào đúng vị trí tháng
        Map<Integer, Integer> hiringMap = new HashMap<>();
        for (Object[] row : rawData) {
            int month = ((Number) row[0]).intValue();
            int count = ((Number) row[1]).intValue();
            hiringMap.put(month, count);
        }

        // Trả về list 6 tháng
        List<Integer> result = new ArrayList<>();
        for (int month : months) {
            result.add(hiringMap.getOrDefault(month, 0));
        }
        return result;
    }

    public List<Integer> getAttritionData() {
        // TODO: tính từ DB sau khi có termination_date
        return Arrays.asList(12, 18, 10, 22, 15, 20);
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
        if (fullName == null || fullName.trim().isEmpty()) return "NA";

        String[] parts = fullName.trim().split("\\s+");

        // Chỉ lấy chữ cái đầu của từ đầu và từ cuối
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }

        return String.valueOf(parts[0].charAt(0)).toUpperCase()
                + String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}