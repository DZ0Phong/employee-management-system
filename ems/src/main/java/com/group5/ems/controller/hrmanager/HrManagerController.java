package com.group5.ems.controller.hrmanager;

import com.group5.ems.service.hrmanager.HRAnalyticsService;
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
    private final HRAnalyticsService analyticsService;  // thêm mới

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("kpi",              dashboardService.getKpiData());
        model.addAttribute("chartMonths",      dashboardService.getChartMonths());
        model.addAttribute("upcomingEvents",   dashboardService.getUpcomingEvents());
        model.addAttribute("recentActivities", dashboardService.getRecentActivities());
        model.addAttribute("activePage",       "dashboard");
        return "hrmanager/dashboard";
    }

    // ── Leave Approval ────────────────────────────────────────────────────────
    @GetMapping("/leave-approval")
    public String leaveApproval(Model model) {
        model.addAttribute("activePage", "leave");
        return "hrmanager/leave_approval";
    }

    // ── Payroll Approval ──────────────────────────────────────────────────────
    @GetMapping("/payroll-approval")
    public String payrollApproval(Model model) {
        model.addAttribute("activePage", "payroll");
        return "hrmanager/payroll_approval";
    }

    // ── HR Analytics ──────────────────────────────────────────────────────────
    @GetMapping("/hr-analytics")
    public String hrAnalytics(Model model) {
        model.addAttribute("kpi",             analyticsService.getKpiData());
        model.addAttribute("deptData",        analyticsService.getDeptData());
        model.addAttribute("salaryData",      analyticsService.getSalaryData());
        model.addAttribute("diversityData",   analyticsService.getDiversityData());
        model.addAttribute("trainingCourses", analyticsService.getTrainingCourses());
        model.addAttribute("policyReviews",   analyticsService.getPolicyReviews());
        model.addAttribute("activePage",      "analytics");
        return "hrmanager/hr_analytics";
    }
}