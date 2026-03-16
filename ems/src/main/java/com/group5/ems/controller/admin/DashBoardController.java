package com.group5.ems.controller.admin;

import com.group5.ems.service.admin.AdminLogService;
import com.group5.ems.service.admin.AdminService;
import com.group5.ems.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashBoardController {

    private final AdminService adminService;
    private final AdminDashboardService adminDashboardService;
    private final AdminLogService adminLogService;

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
        model.addAttribute("deptCount", adminService.getAllDepartmentsCount());

        List<Object []> data = adminDashboardService.getAllDepartmentsPercentage();
        List<String> depChartLabel = new ArrayList<>();
        List<Integer> depCharData = new ArrayList<>();
        for (Object [] row : data) {
            depChartLabel.add(row[0].toString());
            depCharData.add(Integer.parseInt(row[1].toString()));
        }

        model.addAttribute("deptChartLabels", depChartLabel);
        model.addAttribute("deptChartData", depCharData);
        model.addAttribute("deptChartColors", List.of(
                "#1414b8","#38bdf8","#a78bfa","#2dd4bf","#f472b6","#fb923c","#94a3b8","#6366f1"
        ));

        int months = 12;

        model.addAttribute("headcountMonths", adminDashboardService.getHeadcountMonths(months));
        model.addAttribute("headcountTotal", adminDashboardService.getHeadcountTotal(months));
        model.addAttribute("headcountActive", adminDashboardService.getHeadcountActive(months));
        model.addAttribute("headcountSuspended", adminDashboardService.getHeadcountSuspended(months));



        adminService.getUserDTO().ifPresent(u -> model.addAttribute("currentUser", u));

        model.addAttribute("recentUsers", adminDashboardService.getTop5RecentUser());
        model.addAttribute("recentLogs", adminLogService.getRecentLogs(7));

        return "admin/dashboard";
    }

}
