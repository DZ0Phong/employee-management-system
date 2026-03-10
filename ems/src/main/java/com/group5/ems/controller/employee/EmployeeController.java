package com.group5.ems.controller.employee;

import com.group5.ems.dto.response.ActivityDTO;
import com.group5.ems.dto.response.EmployeeDashboardDTO;
import com.group5.ems.dto.response.EmployeeInfoDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.User;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;

import java.util.List;

@Controller
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DashboardService dashboardService;

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
}