package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.DashboardKpiResponseDTO;
import com.group5.ems.dto.response.hrmanager.RecentActivityDTO;
import com.group5.ems.dto.response.hrmanager.UpcomingEventDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.JobPost;
import com.group5.ems.entity.Payslip;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final PayslipRepository payslipRepository;
    private final CalendarService calendarService;
    
    public DashboardKpiResponseDTO getKpiData() {
        DashboardKpiResponseDTO kpi = new DashboardKpiResponseDTO();
        
        // 1. Total Headcount - Tổng số nhân viên đang active
        Long totalHeadcount = employeeRepository.countByStatus("ACTIVE");
        kpi.setTotalHeadcount(totalHeadcount.intValue());
        
        // Calculate headcount change (so với tháng trước)
        LocalDate lastMonthStart = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = lastMonthStart.plusMonths(1).minusDays(1);
        Long lastMonthHeadcount = employeeRepository.countActiveEmployeesAtDate(lastMonthEnd);
        
        if (lastMonthHeadcount > 0) {
            double changePercent = ((totalHeadcount - lastMonthHeadcount) * 100.0) / lastMonthHeadcount;
            kpi.setHeadcountChange(String.format("%+.1f%%", changePercent));
            kpi.setHeadcountChangePositive(changePercent >= 0);
        } else {
            kpi.setHeadcountChange("N/A");
            kpi.setHeadcountChangePositive(true);
        }
        
        // 2. Open Requisitions - Số job posts đang mở
        List<JobPost> openJobs = jobPostRepository.findByStatus("OPEN");
        kpi.setOpenRequisitions(openJobs.size());
        
        // Calculate open requisitions change (so với tháng trước)
        List<JobPost> lastMonthOpenJobs = jobPostRepository.findOpenJobsAtDate(lastMonthEnd);
        int lastMonthOpenCount = lastMonthOpenJobs.size();
        
        if (lastMonthOpenCount > 0) {
            double changePercent = ((openJobs.size() - lastMonthOpenCount) * 100.0) / lastMonthOpenCount;
            kpi.setOpenReqChange(String.format("%+.1f%%", changePercent));
            kpi.setOpenReqChangePositive(changePercent >= 0);
        } else if (openJobs.size() > 0) {
            kpi.setOpenReqChange("New");
            kpi.setOpenReqChangePositive(true);
        } else {
            kpi.setOpenReqChange("0%");
            kpi.setOpenReqChangePositive(true);
        }
        
        // 3. Monthly Turnover - Tính từ số nhân viên nghỉ việc trong tháng
        LocalDate thisMonthStart = LocalDate.now().withDayOfMonth(1);
        Long terminatedThisMonth = employeeRepository.countTerminatedInPeriod(
                thisMonthStart.atStartOfDay(), LocalDate.now().atTime(23, 59, 59));
        
        if (totalHeadcount > 0) {
            double turnoverRate = (terminatedThisMonth * 100.0) / totalHeadcount;
            kpi.setMonthlyTurnover(String.format("%.1f%%", turnoverRate));
            
            // Calculate turnover change (so với tháng trước)
            Long terminatedLastMonth = employeeRepository.countTerminatedInPeriod(
                    lastMonthStart.atStartOfDay(), lastMonthEnd.atTime(23, 59, 59));
            if (lastMonthHeadcount > 0) {
                double lastMonthTurnover = (terminatedLastMonth * 100.0) / lastMonthHeadcount;
                double turnoverChange = turnoverRate - lastMonthTurnover;
                kpi.setTurnoverChange(String.format("%+.1f%%", turnoverChange));
                kpi.setTurnoverChangePositive(turnoverChange <= 0); // Lower is better
            } else {
                kpi.setTurnoverChange("N/A");
                kpi.setTurnoverChangePositive(true);
            }
        } else {
            kpi.setMonthlyTurnover("0.0%");
            kpi.setTurnoverChange("0%");
            kpi.setTurnoverChangePositive(true);
        }
        
        // 4. Average Tenure
        Double avgTenureDays = employeeRepository.getAverageTenureInDays();
        if (avgTenureDays != null && avgTenureDays > 0) {
            double avgTenureYears = avgTenureDays / 365.0;
            kpi.setAverageTenure(String.format("%.1f Yrs", avgTenureYears));
            
            // Calculate tenure change (so sánh với tháng trước)
            Double lastMonthAvgTenureDays = employeeRepository.getAverageTenureInDaysAtDate(lastMonthEnd);
            
            if (lastMonthAvgTenureDays != null && lastMonthAvgTenureDays > 0) {
                double lastMonthAvgTenureYears = lastMonthAvgTenureDays / 365.0;
                double tenureChangeYears = avgTenureYears - lastMonthAvgTenureYears;
                double tenureChangePercent = (tenureChangeYears / lastMonthAvgTenureYears) * 100;
                
                kpi.setTenureChange(String.format("%+.1f%%", tenureChangePercent));
                kpi.setTenureChangePositive(tenureChangePercent >= 0); // Higher is better
            } else {
                kpi.setTenureChange("N/A");
                kpi.setTenureChangePositive(true);
            }
        } else {
            kpi.setAverageTenure("0.0 Yrs");
            kpi.setTenureChange("0%");
            kpi.setTenureChangePositive(true);
        }
        
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

    public Map<String, Object> getRecentActivitiesWithPagination(String filter, int page, int size) {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<Request> allRequests;

        if ("pending".equals(filter)) {
            allRequests = requestRepository.findByStatusWithDetails("PENDING");
        } else if ("approved".equals(filter)) {
            allRequests = requestRepository.findByStatusWithDetails("APPROVED");
        } else {
            allRequests = requestRepository.findRecentActivities(sevenDaysAgo.atStartOfDay());
        }

        // Handle empty list
        if (allRequests == null || allRequests.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("activities", new ArrayList<>());
            result.put("currentPage", 0);
            result.put("totalPages", 0);
            result.put("totalElements", 0);
            result.put("pageSize", size);
            result.put("filter", filter);
            return result;
        }

        // Manual pagination
        int totalElements = allRequests.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Validate page number
        if (page < 0) page = 0;
        if (page >= totalPages) page = Math.max(0, totalPages - 1);
        
        int start = page * size;
        int end = Math.min(start + size, totalElements);

        List<RecentActivityDTO> activities = allRequests.subList(start, end)
                .stream()
                .map(this::mapToRecentActivityDTO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("activities", activities);
        result.put("currentPage", page);
        result.put("totalPages", totalPages);
        result.put("totalElements", totalElements);
        result.put("pageSize", size);
        result.put("filter", filter);

        return result;
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
        // Lấy data 6 tháng gần nhất
        List<Integer> result = new ArrayList<>();
        
        for (int i = 5; i >= 0; i--) {
            LocalDate targetMonth = LocalDate.now().minusMonths(i);
            LocalDate monthStart = targetMonth.withDayOfMonth(1);
            LocalDate monthEnd = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());
            
            // Đếm số nhân viên nghỉ việc trong tháng đó
            Long terminatedCount = employeeRepository.countTerminatedInPeriod(
                    monthStart.atStartOfDay(), monthEnd.atTime(23, 59, 59));
            result.add(terminatedCount.intValue());
        }
        
        return result;
    }
    
    private RecentActivityDTO mapToRecentActivityDTO(Request request) {
        Employee employee = request.getEmployee();
        
        String employeeName = "Unknown";
        String employeePosition = "N/A";
        String employeeInitials = "N/A";
        String department = "N/A";
        
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
            
            if (employee.getDepartment() != null) {
                department = employee.getDepartment().getName();
            }
        }
        
        return RecentActivityDTO.builder()
            .id(request.getId())
            .activityType("REQUEST")
            .employeeName(employeeName)
            .employeePosition(employeePosition)
            .employeeInitials(employeeInitials)
            .department(department)
            .actionLabel(request.getTitle() != null ? request.getTitle() : "Request")
            .details("")
            .date(request.getCreatedAt().toLocalDate())
            .status(request.getStatus())
            .statusLabel(request.getStatus())
            .priority("NORMAL")
            .icon("description")
            .color("blue")
            .badge("")
            .build();
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

    // ══════════════════════════════════════════════════════════════════════════
    // NEW METHODS FOR ACTIVITY CENTER
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Get Quick Stats for Activity Center
     */
    public Map<String, Object> getQuickStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        
        // Leave Requests stats
        long leaveTotal = requestRepository.countByRequestTypeCategory("ATTENDANCE");
        long leavePending = requestRepository.countByStatusAndRequestTypeCategory("PENDING", "ATTENDANCE");
        stats.put("leaveTotal", leaveTotal);
        stats.put("leavePending", leavePending);
        
        // Payroll stats
        List<Payslip> pendingPayslips = payslipRepository.findByStatus("PENDING");
        Map<Long, List<Payslip>> byDept = pendingPayslips.stream()
                .filter(p -> p.getEmployee() != null && p.getEmployee().getDepartment() != null)
                .collect(Collectors.groupingBy(p -> p.getEmployee().getDepartment().getId()));
        
        java.math.BigDecimal payrollTotal = pendingPayslips.stream()
                .map(Payslip::getNetSalary)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        stats.put("payrollTotal", formatCurrency(payrollTotal));
        stats.put("payrollPending", byDept.size());
        
        // Status Changes (this month)
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        long newHires = employeeRepository.findNewHires(monthStart).size();
        long terminations = employeeRepository.findRecentTerminations(monthStart).size();
        stats.put("statusChanges", newHires + terminations);
        
        // HR Requests stats
        long hrTotal = requestRepository.countByRequestTypeCategory("HR");
        long hrPending = requestRepository.countByStatusAndRequestTypeCategory("PENDING", "HR");
        stats.put("hrTotal", hrTotal);
        stats.put("hrPending", hrPending);
        
        // Total items needing action
        stats.put("totalPending", leavePending + byDept.size() + hrPending);
        
        return stats;
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "$0";
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).format(amount);
    }

    /**
     * STEP 4 Part 2: Get Leave Activities
     */
    public List<RecentActivityDTO> getLeaveActivities(LocalDateTime since) {
        List<Request> leaveRequests = requestRepository.findRecentRequestsByCategory("ATTENDANCE", since);
        
        return leaveRequests.stream()
                .map(request -> {
                    Employee employee = request.getEmployee();
                    String employeeName = employee != null && employee.getUser() != null 
                            ? employee.getUser().getFullName() : "Unknown";
                    String employeeInitials = getInitials(employeeName);
                    String department = employee != null && employee.getDepartment() != null 
                            ? employee.getDepartment().getName() : "N/A";
                    String position = employee != null && employee.getPosition() != null 
                            ? employee.getPosition().getName() : "Employee";
                    
                    // Calculate days
                    String details = "";
                    if (request.getLeaveFrom() != null && request.getLeaveTo() != null) {
                        long days = java.time.temporal.ChronoUnit.DAYS.between(
                                request.getLeaveFrom(), request.getLeaveTo()) + 1;
                        String leaveType = request.getLeaveType() != null ? request.getLeaveType() : "Leave";
                        details = leaveType + ": " + days + " day" + (days > 1 ? "s" : "");
                    }
                    
                    // Determine priority
                    String priority = "NORMAL";
                    if ("PENDING".equals(request.getStatus())) {
                        priority = "URGENT";
                    }
                    
                    // Status label
                    String statusLabel = getStatusLabel(request.getStatus());
                    
                    return RecentActivityDTO.builder()
                            .id(request.getId())
                            .activityType("LEAVE")
                            .employeeName(employeeName)
                            .employeePosition(position)
                            .employeeInitials(employeeInitials)
                            .department(department)
                            .actionLabel("Leave Request")
                            .details(details)
                            .date(request.getCreatedAt().toLocalDate())
                            .status(request.getStatus())
                            .statusLabel(statusLabel)
                            .priority(priority)
                            .icon("calendar_today")
                            .color(getColorByStatus(request.getStatus()))
                            .badge("")
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * STEP 4 Part 3: Get HR Request Activities
     */
    public List<RecentActivityDTO> getHRRequestActivities(LocalDateTime since) {
        List<Request> hrRequests = requestRepository.findRecentRequestsByCategory("HR", since);
        
        return hrRequests.stream()
                .map(request -> {
                    Employee employee = request.getEmployee();
                    String employeeName = employee != null && employee.getUser() != null 
                            ? employee.getUser().getFullName() : "Unknown";
                    String employeeInitials = getInitials(employeeName);
                    String department = employee != null && employee.getDepartment() != null 
                            ? employee.getDepartment().getName() : "N/A";
                    String position = employee != null && employee.getPosition() != null 
                            ? employee.getPosition().getName() : "Employee";
                    
                    String actionLabel = request.getTitle() != null ? request.getTitle() : "HR Request";
                    String details = request.getContent() != null && request.getContent().length() > 50 
                            ? request.getContent().substring(0, 50) + "..." 
                            : (request.getContent() != null ? request.getContent() : "");
                    
                    String priority = "PENDING".equals(request.getStatus()) ? "URGENT" : "NORMAL";
                    String statusLabel = getStatusLabel(request.getStatus());
                    
                    return RecentActivityDTO.builder()
                            .id(request.getId())
                            .activityType("HR_REQUEST")
                            .employeeName(employeeName)
                            .employeePosition(position)
                            .employeeInitials(employeeInitials)
                            .department(department)
                            .actionLabel(actionLabel)
                            .details(details)
                            .date(request.getCreatedAt().toLocalDate())
                            .status(request.getStatus())
                            .statusLabel(statusLabel)
                            .priority(priority)
                            .icon("description")
                            .color(getColorByStatus(request.getStatus()))
                            .badge("")
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * STEP 4 Part 4: Get Payroll Activities
     */
    public List<RecentActivityDTO> getPayrollActivities(LocalDateTime since) {
        List<Payslip> payslips = payslipRepository.findRecentPayrollActivities(since);
        
        return payslips.stream()
                .map(payslip -> {
                    Employee employee = payslip.getEmployee();
                    String employeeName = employee != null && employee.getUser() != null 
                            ? employee.getUser().getFullName() : "Unknown";
                    String employeeInitials = getInitials(employeeName);
                    String department = employee != null && employee.getDepartment() != null 
                            ? employee.getDepartment().getName() : "N/A";
                    String position = employee != null && employee.getPosition() != null 
                            ? employee.getPosition().getName() : "Employee";
                    
                    String netSalary = formatCurrency(payslip.getNetSalary());
                    String details = "Net Salary: " + netSalary;
                    
                    String priority = "PENDING".equals(payslip.getStatus()) ? "URGENT" : "NORMAL";
                    String statusLabel = getStatusLabel(payslip.getStatus());
                    
                    // Use paymentDate or current date
                    LocalDate activityDate = payslip.getPaymentDate() != null 
                            ? payslip.getPaymentDate() 
                            : LocalDate.now();
                    
                    return RecentActivityDTO.builder()
                            .id(payslip.getId())
                            .activityType("PAYROLL")
                            .employeeName(employeeName)
                            .employeePosition(position)
                            .employeeInitials(employeeInitials)
                            .department(department)
                            .actionLabel("Payroll")
                            .details(details)
                            .date(activityDate)
                            .status(payslip.getStatus())
                            .statusLabel(statusLabel)
                            .priority(priority)
                            .icon("payments")
                            .color(getColorByStatus(payslip.getStatus()))
                            .badge("")
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * STEP 4 Part 5: Get Status Change Activities (Promotions, Terminations, New Hires)
     */
    public List<RecentActivityDTO> getStatusChangeActivities(LocalDate since) {
        List<RecentActivityDTO> activities = new ArrayList<>();
        
        // New Hires
        List<Employee> newHires = employeeRepository.findNewHires(since);
        for (Employee employee : newHires) {
            String employeeName = employee.getUser() != null ? employee.getUser().getFullName() : "Unknown";
            String employeeInitials = getInitials(employeeName);
            String department = employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A";
            String position = employee.getPosition() != null ? employee.getPosition().getName() : "Employee";
            
            activities.add(RecentActivityDTO.builder()
                    .id(employee.getId())
                    .activityType("STATUS_CHANGE")
                    .employeeName(employeeName)
                    .employeePosition(position)
                    .employeeInitials(employeeInitials)
                    .department(department)
                    .actionLabel("New Hire")
                    .details("Joined as " + position)
                    .date(employee.getHireDate())
                    .status("COMPLETED")
                    .statusLabel("Completed")
                    .priority("NORMAL")
                    .icon("person_add")
                    .color("green")
                    .badge("New hire")
                    .build());
        }
        
        // Terminations
        List<Employee> terminations = employeeRepository.findRecentTerminations(since);
        for (Employee employee : terminations) {
            String employeeName = employee.getUser() != null ? employee.getUser().getFullName() : "Unknown";
            String employeeInitials = getInitials(employeeName);
            String department = employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A";
            String position = employee.getPosition() != null ? employee.getPosition().getName() : "Employee";
            
            String details = employee.getTerminationReason() != null 
                    ? employee.getTerminationReason() 
                    : "Employment terminated";
            
            activities.add(RecentActivityDTO.builder()
                    .id(employee.getId())
                    .activityType("STATUS_CHANGE")
                    .employeeName(employeeName)
                    .employeePosition(position)
                    .employeeInitials(employeeInitials)
                    .department(department)
                    .actionLabel("Termination")
                    .details(details)
                    .date(employee.getTerminationDate())
                    .status("COMPLETED")
                    .statusLabel("Completed")
                    .priority("NORMAL")
                    .icon("person_remove")
                    .color("red")
                    .badge("Termination")
                    .build());
        }
        
        // Promotions (hardcoded for now - check if promotionDate is set)
        List<Employee> allEmployees = employeeRepository.findAll();
        for (Employee employee : allEmployees) {
            if (employee.getPromotionDate() != null && !employee.getPromotionDate().isBefore(since)) {
                String employeeName = employee.getUser() != null ? employee.getUser().getFullName() : "Unknown";
                String employeeInitials = getInitials(employeeName);
                String department = employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A";
                String position = employee.getPosition() != null ? employee.getPosition().getName() : "Employee";
                
                activities.add(RecentActivityDTO.builder()
                        .id(employee.getId())
                        .activityType("STATUS_CHANGE")
                        .employeeName(employeeName)
                        .employeePosition(position)
                        .employeeInitials(employeeInitials)
                        .department(department)
                        .actionLabel("Promotion")
                        .details("Promoted to " + position)
                        .date(employee.getPromotionDate())
                        .status("COMPLETED")
                        .statusLabel("Completed")
                        .priority("NORMAL")
                        .icon("trending_up")
                        .color("purple")
                        .badge("Promotion")
                        .build());
            }
        }
        
        // Sort by date descending
        activities.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        
        return activities;
    }

    /**
     * STEP 4 Part 6: Refactor getRecentActivities to combine all activity types
     */
    public List<RecentActivityDTO> getRecentActivitiesCombined(String filter, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        LocalDate sinceDate = LocalDate.now().minusDays(days);
        
        List<RecentActivityDTO> allActivities = new ArrayList<>();
        
        // Add all activity types
        allActivities.addAll(getLeaveActivities(since));
        allActivities.addAll(getHRRequestActivities(since));
        allActivities.addAll(getPayrollActivities(since));
        allActivities.addAll(getStatusChangeActivities(sinceDate));
        
        // Filter by status if needed
        if ("pending".equals(filter)) {
            allActivities = allActivities.stream()
                    .filter(a -> "PENDING".equals(a.getStatus()))
                    .collect(Collectors.toList());
        } else if ("approved".equals(filter)) {
            allActivities = allActivities.stream()
                    .filter(a -> "APPROVED".equals(a.getStatus()) || "COMPLETED".equals(a.getStatus()))
                    .collect(Collectors.toList());
        }
        
        // Sort by date descending
        allActivities.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        
        return allActivities;
    }

    // Helper methods
    private String getStatusLabel(String status) {
        if (status == null) return "Unknown";
        switch (status.toUpperCase()) {
            case "PENDING": return "Chờ duyệt";
            case "APPROVED": return "Đã duyệt";
            case "REJECTED": return "Từ chối";
            case "COMPLETED": return "Hoàn thành";
            default: return status;
        }
    }

    private String getColorByStatus(String status) {
        if (status == null) return "gray";
        switch (status.toUpperCase()) {
            case "PENDING": return "yellow";
            case "APPROVED": return "green";
            case "REJECTED": return "red";
            case "COMPLETED": return "blue";
            default: return "gray";
        }
    }
    
    /**
     * Get activity categories with counts for dynamic tab navigation
     */
    public List<com.group5.ems.dto.response.hrmanager.ActivityCategoryDTO> getActivityCategories() {
        List<com.group5.ems.dto.response.hrmanager.ActivityCategoryDTO> categories = new ArrayList<>();
        
        // Leave Requests
        long leaveCount = requestRepository.countByStatusAndRequestTypeCategory("PENDING", "ATTENDANCE");
        categories.add(com.group5.ems.dto.response.hrmanager.ActivityCategoryDTO.builder()
                .type("leave")
                .label("Leave Requests")
                .icon("calendar_today")
                .count(leaveCount)
                .color("blue")
                .description("Pending leave requests")
                .build());
        
        // Payroll
        long payrollCount = requestRepository.countByStatusAndRequestTypeCategory("PENDING", "PAYROLL");
        categories.add(com.group5.ems.dto.response.hrmanager.ActivityCategoryDTO.builder()
                .type("payroll")
                .label("Payroll")
                .icon("payments")
                .count(payrollCount)
                .color("green")
                .description("Payroll approvals")
                .build());
        
        // Status Changes
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        long statusCount = employeeRepository.countByUpdatedAtAfter(thirtyDaysAgo.atStartOfDay());
        categories.add(com.group5.ems.dto.response.hrmanager.ActivityCategoryDTO.builder()
                .type("status")
                .label("Status Changes")
                .icon("swap_horiz")
                .count(statusCount)
                .color("purple")
                .description("Employee status updates")
                .build());
        
        // HR Requests
        long hrCount = requestRepository.countByStatusAndRequestTypeCategory("PENDING", "HR_STATUS");
        categories.add(com.group5.ems.dto.response.hrmanager.ActivityCategoryDTO.builder()
                .type("hr")
                .label("HR Requests")
                .icon("description")
                .count(hrCount)
                .color("orange")
                .description("HR document requests")
                .build());
        
        return categories;
    }
}
