package com.group5.ems.controller.hrmanager;

import com.group5.ems.service.hrmanager.HRManagerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hrmanager")
@RequiredArgsConstructor
public class HrManagerController {
    
    private final HRManagerDashboardService dashboardService;
    
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        // Add dashboard data to model
        model.addAttribute("kpi", dashboardService.getKpiData());
        model.addAttribute("chartMonths", dashboardService.getChartMonths());
        model.addAttribute("upcomingEvents", dashboardService.getUpcomingEvents());
        model.addAttribute("recentActivities", dashboardService.getRecentActivities());
        model.addAttribute("activePage", "dashboard");
        
        return "hrmanager/dashboard";
    }
    
    @GetMapping("/dashboard-simple")
    public String dashboardSimple(Model model) {
        // Add dashboard data to model for simple template
        model.addAttribute("kpi", dashboardService.getKpiData());
        model.addAttribute("upcomingEvents", dashboardService.getUpcomingEvents());
        model.addAttribute("recentActivities", dashboardService.getRecentActivities());
        
        return "hrmanager/dashboard_simple";
    }
    
    @GetMapping("/leave-approval")
    public String leaveApproval(Model model) {
        model.addAttribute("activePage", "leave");
        return "hrmanager/leave_approval";
    }
    
    @GetMapping("/payroll-approval")
    public String payrollApproval(Model model) {
        model.addAttribute("activePage", "payroll");
        return "hrmanager/payroll_approval";
    }
    
    @GetMapping("/hr-analytics")
    public String hrAnalytics(Model model) {
        model.addAttribute("activePage", "analytics");
        return "hrmanager/hr_analytics";
    }
}
