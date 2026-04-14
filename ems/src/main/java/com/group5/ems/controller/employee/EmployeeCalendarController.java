package com.group5.ems.controller.employee;

import com.group5.ems.dto.response.hrmanager.EventResponseDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Role;
import com.group5.ems.entity.User;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.repository.UserRoleRepository;
import com.group5.ems.service.hrmanager.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeCalendarController {

    private final CalendarService calendarService;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRoleRepository userRoleRepository;

    // ── Model Attributes ──────────────────────────────────────────────────────
    @ModelAttribute
    public void populateCommonAttributes(Authentication authentication, Model model) {
        if (authentication == null) {
            model.addAttribute("employee", null);
            model.addAttribute("managementPortalUrl", null);
            model.addAttribute("managementPortalLabel", null);
            model.addAttribute("managementRoleLabel", null);
            return;
        }

        User user = getUser(authentication);
        Employee employee = getEmployee(user);
        model.addAttribute("employee", employee);

        // Portal switch logic
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

    // ── Calendar (View Only) ──────────────────────────────────────────────────
    @GetMapping("/calendar")
    public String calendar(Model model,
                           Authentication authentication,
                           @RequestParam(required = false) Integer month,
                           @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        int currentMonth = month != null ? month : now.getMonthValue();
        int currentYear = year != null ? year : now.getYear();

        // Get current user's department
        User user = getUser(authentication);
        Employee employee = getEmployee(user);
        Long userDeptId = employee.getDepartment() != null ? employee.getDepartment().getId() : 1L;
        
        // Get events visible to this department (own dept + company-wide)
        // isHrManager = false → only see own department + company-wide
        List<EventResponseDTO> events = calendarService.getEventsByMonthForUser(
            currentMonth, currentYear, userDeptId, false
        );

        model.addAttribute("events", events);
        model.addAttribute("userDepartment", departmentRepository.findById(userDeptId).orElse(null));
        model.addAttribute("userDepartmentId", userDeptId);
        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("activePage", "calendar");
        model.addAttribute("isEmployee", true);
        model.addAttribute("viewOnly", true); // Employee can only view
        
        return "employee/calendar";
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private User getUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private Employee getEmployee(User user) {
        if (user == null) {
            return null;
        }
        return employeeRepository.findByUserId(user.getId()).orElse(null);
    }
}
