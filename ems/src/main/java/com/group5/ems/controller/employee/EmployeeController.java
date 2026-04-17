package com.group5.ems.controller.employee;

import com.group5.ems.dto.request.CreateLeaveRequestDTO;
import com.group5.ems.dto.request.UpdateProfileRequest;
import com.group5.ems.dto.request.BankDetailsFormDTO;
import com.group5.ems.dto.response.*;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Position;
import com.group5.ems.entity.Role;
import com.group5.ems.entity.User;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.PositionRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.repository.UserRoleRepository;
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
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

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
    private final UserRoleRepository userRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final DashboardService dashboardService;
    private final LeaveServiceImpl leaveService;
    private final ProfileService profileService;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;
    private final PerformanceService performanceService;
    private final EmployeeBankDetailsService employeeBankDetailsService;

    // ── Helper methods ─────────────────────────────────────
    private User getUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Employee getEmployee(User user) {
        return employeeRepository.findByUserId(user.getId()).orElse(null);
    }

    private boolean requiresDepartmentAssignment(User user) {
        List<Role> roles = userRoleRepository.getRolesByUserId(user.getId());
        return roles.stream()
                .map(Role::getCode)
                .anyMatch(code -> "EMPLOYEE".equals(code));
    }

    private boolean hasNoDepartment(Employee employee) {
        return employee == null || employee.getDepartmentId() == null;
    }

    private Employee resolveEmployeeForAttendance(User user) {
        Employee employee = getEmployee(user);
        if (employee != null) {
            return employee;
        }

        List<Role> roles = userRoleRepository.getRolesByUserId(user.getId());
        boolean canAutoProvision = roles.stream()
                .map(Role::getCode)
                .anyMatch(code -> "HR".equals(code) || "HR_MANAGER".equals(code));

        if (!canAutoProvision) {
            return null;
        }

        Department hrDepartment = departmentRepository.findAll().stream()
                .filter(department -> department != null && (
                        matchesCode(department.getCode(), "HR", "HUMAN_RESOURCES", "HUMAN_RESOURCE") ||
                        matchesName(department.getName(), "hr", "human resources")))
                .findFirst()
                .orElse(null);

        if (hrDepartment == null) {
            throw new RuntimeException("No HR department found for this account. Please create or assign an HR department first.");
        }

        Position hrPosition = positionRepository.findAll().stream()
                .filter(position -> position != null)
                .filter(position -> position.getDepartmentId() == null || hrDepartment.getId().equals(position.getDepartmentId()))
                .filter(position -> {
                    String code = position.getCode();
                    String name = position.getName();
                    return matchesCode(code, "HR", "HR_MANAGER", "HR_SPECIALIST", "HR_STAFF")
                            || matchesName(name, "hr", "human resources", "hr manager", "hr specialist");
                })
                .findFirst()
                .orElse(null);

        if (hrPosition == null) {
            throw new RuntimeException("No HR position found for this account. Please create or assign an HR position first.");
        }

        Employee provisioned = new Employee();
        provisioned.setUserId(user.getId());
        provisioned.setEmployeeCode("AUTO-HR-" + user.getId());
        provisioned.setDepartmentId(hrDepartment.getId());
        provisioned.setPositionId(hrPosition.getId());
        provisioned.setHireDate(LocalDate.now());
        provisioned.setStatus("ACTIVE");

        return employeeRepository.save(provisioned);
    }

    private boolean matchesCode(String value, String... candidates) {
        if (value == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate != null && candidate.equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesName(String value, String... keywords) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        for (String keyword : keywords) {
            if (keyword != null && normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
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

    @ModelAttribute
    public void populatePortalSwitch(Authentication authentication, Model model) {
        if (authentication == null) {
            model.addAttribute("managementPortalUrl", null);
            model.addAttribute("managementPortalLabel", null);
            model.addAttribute("managementRoleLabel", null);
            return;
        }

        User user = getUser(authentication);
        List<Role> roles = userRoleRepository.getRolesByUserId(user.getId());

        String portalUrl = null;
        String portalLabel = null;
        String roleLabel = null;
        for (Role role : roles) {
            if (role == null || role.getCode() == null) {
                continue;
            }

            switch (role.getCode()) {
                case "DEPT_MANAGER" -> {
                    portalUrl = "/dept-manager/dashboard";
                    portalLabel = "Department View";
                    roleLabel = role.getName() != null ? role.getName() : "Department Manager";
                }
                case "HR_MANAGER" -> {
                    if (portalUrl == null) {
                        portalUrl = "/hrmanager/dashboard";
                        portalLabel = "HR Manager View";
                        roleLabel = role.getName() != null ? role.getName() : "HR Manager";
                    }
                }
                case "HR" -> {
                    if (portalUrl == null) {
                        portalUrl = "/hr/dashboard";
                        portalLabel = "HR View";
                        roleLabel = role.getName() != null ? role.getName() : "HR Executive";
                    }
                }
                case "ADMIN" -> {
                    if (portalUrl == null) {
                        portalUrl = "/admin/dashboard";
                        portalLabel = "Admin View";
                        roleLabel = role.getName() != null ? role.getName() : "Administrator";
                    }
                }
                default -> {
                }
            }

            if (portalUrl != null && "DEPT_MANAGER".equals(role.getCode())) {
                break;
            }
        }

        model.addAttribute("managementPortalUrl", portalUrl);
        model.addAttribute("managementPortalLabel", portalLabel);
        model.addAttribute("managementRoleLabel", roleLabel);
    }

    // ── Dashboard ──────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);

//        if (requiresDepartmentAssignment(user) && hasNoDepartment(employee)) {
//            model.addAttribute("message", "You have not been assigned to any department yet.");
//            return "common/no-department";
//        }

        employee = resolveEmployeeForAttendance(user);

        if (employee != null) {
            model.addAttribute("employee", dashboardService.getEmployeeInfo(employee.getId(), user.getId()));
            model.addAttribute("dashboard", dashboardService.getDashboardData(employee.getId()));
            model.addAttribute("activities", dashboardService.getRecentActivities(employee.getId()));
        } else {
            model.addAttribute("employee", buildDefaultEmployeeInfo(user));
            model.addAttribute("dashboard", EmployeeDashboardDTO.builder()
                    .leaveBalance(0.0)
                    .leaveBadge("No Balance")
                    .attendanceRate(0.0)
                    .attendanceTrend("+0%")
                    .lastPayroll(0.0)
                    .payrollBadge("No Payroll")
                    .performanceRating(0.0)
                    .performanceBadge("No Review")
                    .trendLabels(List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun"))
                    .trendCurrent(List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
                    .trendPrevious(List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
                    .build());
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
            model.addAttribute("leaveTypes", leaveService.getSupportedLeaveTypes());
        } else {
            model.addAttribute("employee", buildDefaultEmployeeInfo(user));
            model.addAttribute("balances", List.of());
            model.addAttribute("leaveHistory", List.of());
            model.addAttribute("leaveTypes", List.of());
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

    @PostMapping("/leave/{id}/cancel")
    public String cancelLeaveRequest(@PathVariable Long id,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            User user = getUser(authentication);
            Employee employee = getEmployee(user);
            if (employee == null) throw new RuntimeException("You are not assigned to any department yet.");
            leaveService.cancelLeaveRequest(employee.getId(), id);
            redirectAttributes.addFlashAttribute("success", "Leave request cancelled successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel leave request: " + e.getMessage());
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
            Employee employee = resolveEmployeeForAttendance(user);
            if (employee == null) throw new RuntimeException("No employee profile found for this account.");
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
            Employee employee = resolveEmployeeForAttendance(user);
            if (employee == null) throw new RuntimeException("No employee profile found for this account.");
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
        Employee employee = resolveEmployeeForAttendance(user);
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
            PayrollSummaryDTO summary = payrollService.getPayrollSummary(employee.getId());
            BankDetailsResponseDTO primaryBankDetail = employeeBankDetailsService.getPrimaryBankDetail(employee.getId());
            payrollService.applyPrimaryBankDetail(summary, primaryBankDetail);
            model.addAttribute("employee", dashboardService.getEmployeeInfo(employee.getId(), user.getId()));
            model.addAttribute("summary", summary);
            model.addAttribute("payslips", payrollService.getPayslipHistory(employee.getId()));
            model.addAttribute("bankDetails", employeeBankDetailsService.getBankDetails(employee.getId()));
            model.addAttribute("banks", employeeBankDetailsService.getSupportedBanks());
            if (!model.containsAttribute("bankDetailsForm")) {
                model.addAttribute("bankDetailsForm", new BankDetailsFormDTO());
            }
        } else {
            model.addAttribute("employee", buildDefaultEmployeeInfo(user));
            model.addAttribute("summary", PayrollSummaryDTO.builder().hasBankDetails(false).build());
            model.addAttribute("payslips", List.of());
            model.addAttribute("bankDetails", List.of());
            model.addAttribute("banks", List.of());
            if (!model.containsAttribute("bankDetailsForm")) {
                model.addAttribute("bankDetailsForm", new BankDetailsFormDTO());
            }
        }

        return "employee/payroll";
    }

    @PostMapping("/payroll/bank-details/add")
    public String addPayrollBankDetails(Authentication authentication,
                                        @Valid @ModelAttribute("bankDetailsForm") BankDetailsFormDTO form,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("payrollError", "Employee profile not found.");
            return "redirect:/employee/payroll";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("payrollError", "Please review your bank details and try again.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.bankDetailsForm", bindingResult);
            redirectAttributes.addFlashAttribute("bankDetailsForm", form);
            return "redirect:/employee/payroll";
        }
        try {
            employeeBankDetailsService.addBankDetails(employee.getId(), form);
            redirectAttributes.addFlashAttribute("payrollSuccess", "Bank account saved successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("payrollError", ex.getMessage());
        }
        return "redirect:/employee/payroll";
    }

    @PostMapping("/payroll/bank-details/{bankId}/set-primary")
    public String setPayrollPrimaryBank(Authentication authentication,
                                        @PathVariable Long bankId,
                                        RedirectAttributes redirectAttributes) {
        User user = getUser(authentication);
        Employee employee = getEmployee(user);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("payrollError", "Employee profile not found.");
            return "redirect:/employee/payroll";
        }
        try {
            employeeBankDetailsService.setPrimaryAccount(employee.getId(), bankId);
            redirectAttributes.addFlashAttribute("payrollSuccess", "Primary payout account updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("payrollError", ex.getMessage());
        }
        return "redirect:/employee/payroll";
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
                    .totalReviews(0)
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
