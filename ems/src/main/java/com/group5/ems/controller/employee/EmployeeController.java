package com.group5.ems.controller.employee;

import com.group5.ems.dto.request.CreateLeaveRequestDTO;
import com.group5.ems.dto.request.UpdateProfileRequest;
import com.group5.ems.dto.response.*;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.User;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.employee.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DashboardService dashboardService;
    private final LeaveServiceImpl leaveService;
    private final ProfileService profileService;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;
    private final PerformanceService performanceService;

    // ── Helper methods ─────────────────────────────────────
    private User getUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Employee getEmployee(User user) {
        return employeeRepository.findByUserId(user.getId()).orElse(null);
    }

    // Tạo EmployeeInfoDTO mặc định khi chưa được assign phòng ban
    private EmployeeInfoDTO buildDefaultEmployeeInfo(User user) {
        return EmployeeInfoDTO.builder()
                .fullName(user.getFullName())
                .firstName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .position("Unassigned")
                .department("—")
                .build();
    }

    // ── Dashboard ──────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);

        if (employee != null) {
            model.addAttribute("employee", dashboardService.getEmployeeInfo(employee.getId(), user.getId()));
            model.addAttribute("dashboard", dashboardService.getDashboardData(employee.getId()));
            model.addAttribute("activities", dashboardService.getRecentActivities(employee.getId()));
        } else {
            model.addAttribute("employee", buildDefaultEmployeeInfo(user));
            model.addAttribute("dashboard", EmployeeDashboardDTO.builder()
                    .leaveBalance(0.0).attendanceRate(0.0)
                    .attendanceTrend("+0%").lastPayroll(0.0)
                    .performanceRating(0.0).build());
            model.addAttribute("activities", List.of());
        }

        return "employee/dashboard";
    }

    // ── Leave ──────────────────────────────────────────────
    @GetMapping("/leave")
    public String leave(Model model, Authentication authentication) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);

        if (employee != null) {
            model.addAttribute("employee", dashboardService.getEmployeeInfo(employee.getId(), user.getId()));
            model.addAttribute("balances", leaveService.getLeaveBalances(employee.getId()));
            model.addAttribute("leaveHistory", leaveService.getLeaveHistory(employee.getId()));
        } else {
            model.addAttribute("employee", buildDefaultEmployeeInfo(user));
            model.addAttribute("balances", List.of());
            model.addAttribute("leaveHistory", List.of());
        }

        return "employee/leave";
    }

    @PostMapping("/leave/request")
    public String createLeaveRequest(@ModelAttribute CreateLeaveRequestDTO dto,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            User user = getUser(authentication);
            Employee employee = getEmployee(user);
            if (employee == null) throw new RuntimeException("You are not assigned to any department yet.");
            leaveService.createLeaveRequest(employee.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Leave request submitted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit leave request: " + e.getMessage());
        }
        return "redirect:/employee/leave";
    }

    // ── Profile ────────────────────────────────────────────
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);

        if (employee != null) {
            model.addAttribute("profile", profileService.getProfile(employee.getId(), user.getId()));
            model.addAttribute("employee", dashboardService.getEmployeeInfo(employee.getId(), user.getId()));
        } else {
            model.addAttribute("profile", EmployeeProfileDTO.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .phone(user.getPhone())
                    .avatarUrl(user.getAvatarUrl())
                    .status(user.getStatus())
                    .departmentName("Unassigned")
                    .positionName("Unassigned")
                    .build());
            model.addAttribute("employee", buildDefaultEmployeeInfo(user));
        }

        return "employee/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication,
                                @ModelAttribute UpdateProfileRequest dto,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = getUser(authentication);
            profileService.updateProfile(user.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }
        return "redirect:/employee/profile";
    }

    // ── Attendance ─────────────────────────────────────────
    @GetMapping("/attendance")
    public String attendance(Authentication authentication, Model model,
                             @RequestParam(defaultValue = "0") int year,
                             @RequestParam(defaultValue = "0") int month) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);

        LocalDate now = LocalDate.now();
        int selectedYear = year == 0 ? now.getYear() : year;
        int selectedMonth = month == 0 ? now.getMonthValue() : month;

        List<Integer> years = List.of(now.getYear() - 2, now.getYear() - 1, now.getYear());
        List<Map<String, Object>> months = new ArrayList<>();
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("value", i);
            m.put("label", monthNames[i - 1]);
            months.add(m);
        }

        if (employee != null) {
            model.addAttribute("employee", dashboardService.getEmployeeInfo(employee.getId(), user.getId()));
            model.addAttribute("attendances", attendanceService.getAttendanceHistory(employee.getId(), selectedYear, selectedMonth));
            model.addAttribute("stats", attendanceService.getAttendanceStats(employee.getId(), selectedYear, selectedMonth));
        } else {
            model.addAttribute("employee", buildDefaultEmployeeInfo(user));
            model.addAttribute("attendances", List.of());
            model.addAttribute("stats", AttendanceStatsDTO.builder()
                    .totalHours("0h 0m").onTimeRate(0.0)
                    .presentDays(0).totalWorkDays(0)
                    .clockedInToday(false).clockedOutToday(false).build());
        }

        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("years", years);
        model.addAttribute("months", months);

        return "employee/attendance";
    }

    @PostMapping("/attendance/clock-in")
    public String clockIn(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getUser(authentication);
            Employee employee = getEmployee(user);
            if (employee == null) throw new RuntimeException("You are not assigned to any department yet.");
            attendanceService.clockIn(employee.getId());
            redirectAttributes.addFlashAttribute("success", "Clocked in successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employee/attendance";
    }

    @PostMapping("/attendance/clock-out")
    public String clockOut(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getUser(authentication);
            Employee employee = getEmployee(user);
            if (employee == null) throw new RuntimeException("You are not assigned to any department yet.");
            attendanceService.clockOut(employee.getId());
            redirectAttributes.addFlashAttribute("success", "Clocked out successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employee/attendance";
    }

    @GetMapping("/attendance/export")
    public ResponseEntity<byte[]> exportAttendance(Authentication authentication,
                                                   @RequestParam int year,
                                                   @RequestParam int month) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);
        if (employee == null) {
            return ResponseEntity.badRequest().build();
        }

        byte[] csv = attendanceService.exportReport(employee.getId(), year, month);
        String filename = "attendance_" + year + "_" + month + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ── Payroll ────────────────────────────────────────────
    @GetMapping("/payroll")
    public String payroll(Authentication authentication, Model model) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);

        if (employee != null) {
            model.addAttribute("employee", dashboardService.getEmployeeInfo(employee.getId(), user.getId()));
            model.addAttribute("summary", payrollService.getPayrollSummary(employee.getId()));
            model.addAttribute("payslips", payrollService.getPayslipHistory(employee.getId()));
        } else {
            model.addAttribute("employee", buildDefaultEmployeeInfo(user));
            model.addAttribute("summary", PayrollSummaryDTO.builder().build());
            model.addAttribute("payslips", List.of());
        }

        return "employee/payroll";
    }

    @GetMapping("/payroll/export")
    public ResponseEntity<byte[]> exportPayroll(Authentication authentication) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);
        if (employee == null) {
            return ResponseEntity.badRequest().build();
        }

        byte[] csv = payrollService.exportPayslipCsv(employee.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payroll_history.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ── Performance ────────────────────────────────────────
    @GetMapping("/performance")
    public String performance(Authentication authentication, Model model) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);

        if (employee != null) {
            model.addAttribute("employee", dashboardService.getEmployeeInfo(employee.getId(), user.getId()));
            model.addAttribute("summary", performanceService.getPerformanceSummary(employee.getId()));
            model.addAttribute("reviews", performanceService.getReviewHistory(employee.getId()));
        } else {
            model.addAttribute("employee", buildDefaultEmployeeInfo(user));
            model.addAttribute("summary", PerformanceSummaryDTO.builder()
                    .currentRating(java.math.BigDecimal.ZERO)
                    .previousRating(java.math.BigDecimal.ZERO)
                    .talentMatrix("N/A").totalReviews(0)
                    .kpisMet(0).kpisTotal(0).skillsCount(0).build());
            model.addAttribute("reviews", List.of());
        }

        return "employee/performance";
    }

    // ── Settings ───────────────────────────────────────────
    @GetMapping("/settings")
    public String settings(Authentication authentication, Model model) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);

        model.addAttribute("employee", employee != null
                ? dashboardService.getEmployeeInfo(employee.getId(), user.getId())
                : buildDefaultEmployeeInfo(user));

        return "employee/settings";
    }
}