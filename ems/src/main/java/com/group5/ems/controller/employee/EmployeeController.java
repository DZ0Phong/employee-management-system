package com.group5.ems.controller.employee;

import com.group5.ems.dto.request.CreateLeaveRequestDTO;
import com.group5.ems.dto.response.ActivityDTO;
import com.group5.ems.dto.response.EmployeeDashboardDTO;
import com.group5.ems.dto.response.EmployeeInfoDTO;
import com.group5.ems.dto.response.LeaveBalanceDTO;
import com.group5.ems.dto.response.LeaveRequestDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.User;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.employee.DashboardService;
import com.group5.ems.service.employee.LeaveServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.group5.ems.dto.request.UpdateProfileRequest;
import com.group5.ems.dto.response.EmployeeProfileDTO;
import com.group5.ems.service.employee.ProfileService;
import java.util.List;

@Controller
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DashboardService dashboardService;
    private final LeaveServiceImpl leaveService;
    private final ProfileService profileService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        EmployeeInfoDTO employeeInfo = dashboardService.getEmployeeInfo(employee.getId(), user.getId());
        EmployeeDashboardDTO dashboardData = dashboardService.getDashboardData(employee.getId());
        List<ActivityDTO> activities = dashboardService.getRecentActivities(employee.getId());

        model.addAttribute("employee", employeeInfo);
        model.addAttribute("dashboard", dashboardData);
        model.addAttribute("activities", activities);

        return "employee/dashboard";
    }

    @GetMapping("/leave")
    public String leave(Model model, Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        EmployeeInfoDTO employeeInfo = dashboardService.getEmployeeInfo(employee.getId(), user.getId());
        List<LeaveBalanceDTO> balances = leaveService.getLeaveBalances(employee.getId());
        List<LeaveRequestDTO> history = leaveService.getLeaveHistory(employee.getId());

        model.addAttribute("employee", employeeInfo);
        model.addAttribute("balances", balances);
        model.addAttribute("history", history);

        return "employee/leave";
    }

    @PostMapping("/leave/request")
    public String createLeaveRequest(@ModelAttribute CreateLeaveRequestDTO dto,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Employee employee = employeeRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            leaveService.createLeaveRequest(employee.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Leave request submitted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit leave request: " + e.getMessage());
        }

        return "redirect:/employee/leave";
    }


    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        EmployeeProfileDTO profile = profileService.getProfile(employee.getId(), user.getId());

        model.addAttribute("profile", profile);
        model.addAttribute("employee", dashboardService.getEmployeeInfo(employee.getId(), user.getId()));

        return "employee/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication,
                                @ModelAttribute UpdateProfileRequest dto,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            profileService.updateProfile(user.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }
        return "redirect:/employee/profile";
    }
}