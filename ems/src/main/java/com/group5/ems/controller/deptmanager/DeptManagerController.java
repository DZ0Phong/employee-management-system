package com.group5.ems.controller.deptmanager;

import com.group5.ems.service.deptmanager.DeptManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dept-manager")
@RequiredArgsConstructor
public class DeptManagerController {

    private final DeptManagerService deptManagerService;

    @GetMapping({ "", "/", "/dashboard" })
    public String dashboard(Model model) {
        // Fetch mock data
        model.addAllAttributes(deptManagerService.getDashboardMockData());

        // Return the thymeleaf template
        return "deptmanager/dashboard";
    }

    @GetMapping("/team")
    public String myTeam(Model model) {
        // Fetch mock data for the team page
        model.addAllAttributes(deptManagerService.getTeamMockData());

        // Return the thymeleaf template
        return "deptmanager/team";
    }

    @GetMapping("/department")
    public String myDepartment(Model model) {
        // Fetch mock data for the department organizational page
        model.addAllAttributes(deptManagerService.getDepartmentMockData());

        // Return the thymeleaf template
        return "deptmanager/department";
    }
}
