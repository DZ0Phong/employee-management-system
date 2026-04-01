package com.group5.ems.service.employee;

import com.group5.ems.dto.response.ActivityDTO;
import com.group5.ems.dto.response.EmployeeDashboardDTO;
import com.group5.ems.dto.response.EmployeeInfoDTO;
import com.group5.ems.entity.Attendance;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.PerformanceReview;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.User;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.PerformanceReviewRepository;
import com.group5.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PerformanceReviewRepository performanceReviewRepository;

    @Override
    @Transactional(readOnly = true)
    public EmployeeInfoDTO getEmployeeInfo(Long employeeId, Long userId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String firstName = "";
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            String[] parts = user.getFullName().trim().split("\\s+");
            firstName = parts[parts.length - 1];
        }

        String positionName = employee.getPosition() != null ? employee.getPosition().getName() : "Staff";
        String departmentName = employee.getDepartment() != null ? employee.getDepartment().getName() : "";

        return EmployeeInfoDTO.builder()
                .id(employeeId)
                .fullName(user.getFullName())
                .firstName(firstName)
                .avatarUrl(user.getAvatarUrl())
                .position(positionName)
                .department(departmentName)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDashboardDTO getDashboardData(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        double leaveBalance = calculateLeaveBalance(employee);
        double attendanceRate = calculateAttendanceRate(employee);
        double lastPayroll = getLastPayroll(employee);
        double performanceRating = getPerformanceRating(employee);
        List<YearMonth> recentMonths = buildRecentMonths();

        return EmployeeDashboardDTO.builder()
                .leaveBalance(leaveBalance)
                .attendanceRate(attendanceRate)
                .attendanceTrend(calculateAttendanceTrend(employee, recentMonths))
                .lastPayroll(lastPayroll)
                .performanceRating(performanceRating)
                .leaveBadge(resolveLeaveBadge(leaveBalance))
                .payrollBadge(lastPayroll > 0 ? "Processed" : "No Payslip")
                .performanceBadge(resolvePerformanceBadge(performanceRating))
                .trendLabels(recentMonths.stream()
                        .map(month -> month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                        .toList())
                .trendCurrent(buildAttendanceTrend(employee, recentMonths, 0))
                .trendPrevious(buildAttendanceTrend(employee, recentMonths, 1))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getRecentActivities(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Stream<ActivityWithDate> requestActivities = employee.getRequests() == null
                ? Stream.empty()
                : employee.getRequests().stream()
                        .map(req -> new ActivityWithDate(
                                mapRequestToActivity(req),
                                req.getCreatedAt() != null ? req.getCreatedAt() : LocalDateTime.MIN));

        Stream<ActivityWithDate> attendanceActivities = employee.getAttendances() == null
                ? Stream.empty()
                : employee.getAttendances().stream()
                        .map(att -> new ActivityWithDate(
                                mapAttendanceToActivity(att),
                                att.getUpdatedAt() != null ? att.getUpdatedAt() : att.getWorkDate().atStartOfDay()));

        return Stream.concat(requestActivities, attendanceActivities)
                .sorted(Comparator.comparing(ActivityWithDate::timestamp).reversed())
                .limit(5)
                .map(ActivityWithDate::activity)
                .toList();
    }

    private double calculateLeaveBalance(Employee employee) {
        double total = 12.0;
        if (employee.getRequests() == null) {
            return total;
        }

        double used = employee.getRequests().stream()
                .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getLeaveFrom() != null && r.getLeaveTo() != null)
                .mapToDouble(r -> ChronoUnit.DAYS.between(r.getLeaveFrom(), r.getLeaveTo()) + 1)
                .sum();

        return Math.max(0, total - used);
    }

    private double calculateAttendanceRate(Employee employee) {
        if (employee.getAttendances() == null || employee.getAttendances().isEmpty()) {
            return 0.0;
        }

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);

        long total = employee.getAttendances().stream()
                .filter(a -> !a.getWorkDate().isBefore(startOfMonth) && !a.getWorkDate().isAfter(now))
                .count();

        long present = employee.getAttendances().stream()
                .filter(a -> !a.getWorkDate().isBefore(startOfMonth) && !a.getWorkDate().isAfter(now))
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus()))
                .count();

        if (total == 0) {
            return 0.0;
        }
        return Math.round((present * 1000.0 / total)) / 10.0;
    }

    private double getLastPayroll(Employee employee) {
        if (employee.getSalaries() == null || employee.getSalaries().isEmpty()) {
            return 0.0;
        }

        return employee.getSalaries().stream()
                .filter(salary -> salary.getEffectiveTo() == null || !salary.getEffectiveTo().isBefore(LocalDate.now()))
                .mapToDouble(salary -> salary.getBaseAmount().doubleValue()
                        + (salary.getAllowanceAmount() != null ? salary.getAllowanceAmount().doubleValue() : 0))
                .findFirst()
                .orElse(0.0);
    }

    private double getPerformanceRating(Employee employee) {
        return performanceReviewRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                .map(PerformanceReview::getPerformanceScore)
                .filter(score -> score != null)
                .findFirst()
                .map(score -> score.doubleValue())
                .orElse(0.0);
    }

    private List<YearMonth> buildRecentMonths() {
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            months.add(currentMonth.minusMonths(i));
        }
        return months;
    }

    private List<Double> buildAttendanceTrend(Employee employee, List<YearMonth> months, int yearOffset) {
        return months.stream()
                .map(month -> month.minusYears(yearOffset))
                .map(targetMonth -> attendanceRateForMonth(employee, targetMonth))
                .toList();
    }

    private double attendanceRateForMonth(Employee employee, YearMonth month) {
        long total = employee.getAttendances().stream()
                .filter(att -> YearMonth.from(att.getWorkDate()).equals(month))
                .count();
        long present = employee.getAttendances().stream()
                .filter(att -> YearMonth.from(att.getWorkDate()).equals(month))
                .filter(att -> "PRESENT".equalsIgnoreCase(att.getStatus()) || "LATE".equalsIgnoreCase(att.getStatus()))
                .count();

        if (total == 0) {
            return 0.0;
        }
        return Math.round((present * 1000.0 / total)) / 10.0;
    }

    private String calculateAttendanceTrend(Employee employee, List<YearMonth> recentMonths) {
        if (recentMonths.isEmpty()) {
            return "0.0%";
        }
        YearMonth currentMonth = recentMonths.get(recentMonths.size() - 1);
        double currentRate = attendanceRateForMonth(employee, currentMonth);
        double previousRate = attendanceRateForMonth(employee, currentMonth.minusMonths(1));
        double delta = currentRate - previousRate;
        String sign = delta > 0 ? "+" : "";
        return sign + String.format(Locale.ENGLISH, "%.1f%%", delta);
    }

    private String resolveLeaveBadge(double leaveBalance) {
        if (leaveBalance >= 8) {
            return "Healthy";
        }
        if (leaveBalance >= 4) {
            return "Moderate";
        }
        return "Low";
    }

    private String resolvePerformanceBadge(double performanceRating) {
        if (performanceRating >= 4.5) {
            return "Exceeds";
        }
        if (performanceRating >= 3.5) {
            return "On Track";
        }
        if (performanceRating > 0) {
            return "Needs Focus";
        }
        return "No Review";
    }

    private ActivityDTO mapRequestToActivity(Request req) {
        String typeName = req.getRequestType() != null ? req.getRequestType().getName() : "Request";
        String step = req.getStep() != null ? req.getStep() : "WAITING_DM";
        String statusLabel = switch (req.getStatus() != null ? req.getStatus().toUpperCase(Locale.ENGLISH) : "PENDING") {
            case "APPROVED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            default -> switch (step) {
                case "WAITING_HR" -> "WAITING FOR HR";
                case "WAITING_HRM" -> "WAITING FOR HR MANAGER";
                default -> "WAITING FOR MANAGER";
            };
        };

        String icon;
        String iconBg;
        String iconColor;

        switch (req.getStatus() != null ? req.getStatus().toUpperCase(Locale.ENGLISH) : "PENDING") {
            case "APPROVED" -> {
                icon = "check_circle";
                iconBg = "bg-emerald-100";
                iconColor = "text-emerald-600";
            }
            case "REJECTED" -> {
                icon = "cancel";
                iconBg = "bg-red-100";
                iconColor = "text-red-600";
            }
            default -> {
                icon = "schedule";
                iconBg = "bg-amber-100";
                iconColor = "text-amber-600";
            }
        }

        return ActivityDTO.builder()
                .title(typeName + " — " + statusLabel)
                .description(req.getTitle() != null ? req.getTitle() : "No details")
                .icon(icon)
                .iconBg(iconBg)
                .iconColor(iconColor)
                .timeAgo(formatTimeAgo(req.getCreatedAt()))
                .build();
    }

    private ActivityDTO mapAttendanceToActivity(Attendance att) {
        String status = att.getStatus() != null ? att.getStatus().toUpperCase(Locale.ENGLISH) : "PRESENT";
        return ActivityDTO.builder()
                .title("Attendance — " + status)
                .description("Work date: " + att.getWorkDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .icon(resolveAttendanceIcon(status))
                .iconBg(resolveAttendanceIconBg(status))
                .iconColor(resolveAttendanceIconColor(status))
                .timeAgo(formatTimeAgo(att.getUpdatedAt() != null ? att.getUpdatedAt() : att.getWorkDate().atStartOfDay()))
                .build();
    }

    private String resolveAttendanceIcon(String status) {
        return switch (status) {
            case "ABSENT" -> "cancel";
            case "LATE" -> "schedule";
            default -> "check_circle";
        };
    }

    private String resolveAttendanceIconBg(String status) {
        return switch (status) {
            case "ABSENT" -> "bg-red-100";
            case "LATE" -> "bg-amber-100";
            default -> "bg-emerald-100";
        };
    }

    private String resolveAttendanceIconColor(String status) {
        return switch (status) {
            case "ABSENT" -> "text-red-600";
            case "LATE" -> "text-amber-600";
            default -> "text-emerald-600";
        };
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) {
            return "Just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " hr ago";
        }
        long days = hours / 24;
        if (days == 1) {
            return "Yesterday";
        }
        return days + " days ago";
    }

    private record ActivityWithDate(ActivityDTO activity, LocalDateTime timestamp) {
    }
}
