package com.group5.ems.controller.admin;

import com.group5.ems.service.admin.AdminService;
import com.group5.ems.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashBoardController {

    private final AdminService adminService;
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        //department side bar
        model.addAttribute("statTotal", adminService.getAllDepartmentsCount());
        model.addAttribute("statActiveEmployees", adminDashboardService.getAllActiveEmployees());
        model.addAttribute("statInactiveEmployees", adminDashboardService.getAllInactiveEmployees());
        model.addAttribute("statSuspendedEmployees", adminDashboardService.getAllSuspendedEmployees());
        model.addAttribute("statTotalStaff", adminDashboardService.getAllEmployeesCount());
        model.addAttribute("statNewThisMonth", adminDashboardService.getNewThisMonth());
        model.addAttribute("statActiveRate", adminDashboardService.getActiveRate());
        model.addAttribute("statInactiveRate", adminDashboardService.getInactiveRate());
        model.addAttribute("statSuspendedRate", adminDashboardService.getSuspendedRate());

        adminService.getUserDTO().ifPresent(u -> model.addAttribute("currentUser", u));
        return "admin/dashboard";
    }
}
