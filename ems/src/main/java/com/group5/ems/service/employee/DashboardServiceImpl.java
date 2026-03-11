package com.group5.ems.service.employee;

import com.group5.ems.dto.response.ActivityDTO;
import com.group5.ems.dto.response.EmployeeDashboardDTO;
import com.group5.ems.dto.response.EmployeeInfoDTO;
import com.group5.ems.entity.*;
import com.group5.ems.repository.*;
import com.group5.ems.service.employee.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

        private final EmployeeRepository employeeRepository;
        private final UserRepository userRepository;

        @Override
        @Transactional
        public EmployeeInfoDTO getEmployeeInfo(Long employeeId, Long userId) {
                Employee employee = employeeRepository.findById(employeeId)
                                .orElseThrow(() -> new RuntimeException("Employee not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String firstName = "";
                if (user.getFullName() != null && !user.getFullName().isBlank()) {
                        String[] parts = user.getFullName().trim().split("\\s+");
                        firstName = parts[parts.length - 1]; // "Nguyen Van A" -> "A"
                }

                String positionName = (employee.getPosition() != null)
                                ? employee.getPosition().getName()
                                : "Staff";

                String departmentName = (employee.getDepartment() != null)
                                ? employee.getDepartment().getName()
                                : "";

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
        @Transactional
        public EmployeeDashboardDTO getDashboardData(Long employeeId) {
                Employee employee = employeeRepository.findById(employeeId)
                                .orElseThrow(() -> new RuntimeException("Employee not found"));

                double leaveBalance = calculateLeaveBalance(employee);
                double attendanceRate = calculateAttendanceRate(employee);
                double lastPayroll = getLastPayroll(employee);
                double performanceRating = getPerformanceRating(employee);

                return EmployeeDashboardDTO.builder()
                                .leaveBalance(leaveBalance)
                                .attendanceRate(attendanceRate)
                                .attendanceTrend("+0%")
                                .lastPayroll(lastPayroll)
                                .performanceRating(performanceRating)
                                .build();
        }

        @Override
        @Transactional
        public List<ActivityDTO> getRecentActivities(Long employeeId) {
                Employee employee = employeeRepository.findById(employeeId)
                                .orElseThrow(() -> new RuntimeException("Employee not found"));

                List<ActivityDTO> activities = new ArrayList<>();

                // Activity từ requests gần nhất
                if (employee.getRequests() != null) {
                        employee.getRequests().stream()
                                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                        .limit(5)
                                        .forEach(req -> activities.add(mapRequestToActivity(req)));
                }

                // Activity từ attendance nếu chưa đủ 5
                if (activities.size() < 5 && employee.getAttendances() != null) {
                        employee.getAttendances().stream()
                                        .sorted((a, b) -> b.getWorkDate().compareTo(a.getWorkDate()))
                                        .limit(5 - activities.size())
                                        .forEach(att -> activities.add(mapAttendanceToActivity(att)));
                }

                return activities;
        }

        // ── Helper methods ──────────────────────────────────────

        private double calculateLeaveBalance(Employee employee) {
                double total = 12.0;
                if (employee.getRequests() == null)
                        return total;

                // Tính số ngày leave đã APPROVED dựa trên leaveFrom / leaveTo
                double used = employee.getRequests().stream()
                                .filter(r -> "APPROVED".equals(r.getStatus()))
                                .filter(r -> r.getLeaveFrom() != null && r.getLeaveTo() != null)
                                .mapToDouble(r -> ChronoUnit.DAYS.between(r.getLeaveFrom(), r.getLeaveTo()) + 1)
                                .sum();

                return Math.max(0, total - used);
        }

        private double calculateAttendanceRate(Employee employee) {
                if (employee.getAttendances() == null || employee.getAttendances().isEmpty())
                        return 0.0;

                LocalDate now = LocalDate.now();
                LocalDate startOfMonth = now.withDayOfMonth(1);

                long total = employee.getAttendances().stream()
                                .filter(a -> !a.getWorkDate().isBefore(startOfMonth) && !a.getWorkDate().isAfter(now))
                                .count();

                long present = employee.getAttendances().stream()
                                .filter(a -> !a.getWorkDate().isBefore(startOfMonth) && !a.getWorkDate().isAfter(now))
                                .filter(a -> "PRESENT".equals(a.getStatus()))
                                .count();

                if (total == 0)
                        return 0.0;
                return Math.round((present * 100.0 / total) * 10.0) / 10.0;
        }

        private double getLastPayroll(Employee employee) {
                if (employee.getSalaries() == null || employee.getSalaries().isEmpty())
                        return 0.0;

                return employee.getSalaries().stream()
                                .filter(s -> s.getEffectiveTo() == null
                                                || !s.getEffectiveTo().isBefore(LocalDate.now()))
                                .mapToDouble(s -> s.getBaseAmount().doubleValue()
                                                + (s.getAllowanceAmount() != null ? s.getAllowanceAmount().doubleValue()
                                                                : 0))
                                .findFirst()
                                .orElse(0.0);
        }

        private double getPerformanceRating(Employee employee) {
                // TODO: inject PerformanceReviewRepository khi có
                return 0.0;
        }

        private ActivityDTO mapRequestToActivity(Request req) {
                String typeName = req.getRequestType() != null
                                ? req.getRequestType().getName()
                                : "Request";
                String status = req.getStatus();

                String icon;
                String iconBg;
                String iconColor;

                switch (status) {
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
                                icon = "pending";
                                iconBg = "bg-yellow-100";
                                iconColor = "text-yellow-600";
                        }
                }

                return ActivityDTO.builder()
                                .title(typeName + " — " + status)
                                .description(req.getTitle() != null ? req.getTitle() : "No details")
                                .icon(icon)
                                .iconBg(iconBg)
                                .iconColor(iconColor)
                                .timeAgo(formatTimeAgo(req.getCreatedAt()))
                                .build();
        }

        private ActivityDTO mapAttendanceToActivity(Attendance att) {
                return ActivityDTO.builder()
                                .title("Checked In")
                                .description("Work date: " + att.getWorkDate()
                                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                                .icon("schedule")
                                .iconBg("bg-slate-100")
                                .iconColor("text-slate-600")
                                .timeAgo(att.getWorkDate().toString())
                                .build();
        }

        private String formatTimeAgo(LocalDateTime dateTime) {
                if (dateTime == null)
                        return "";
                long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
                if (minutes < 1)
                        return "Just now";
                if (minutes < 60)
                        return minutes + " min ago";
                long hours = minutes / 60;
                if (hours < 24)
                        return hours + " hr ago";
                long days = hours / 24;
                if (days == 1)
                        return "Yesterday";
                return days + " days ago";
        }
}