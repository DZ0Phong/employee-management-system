package com.group5.ems.controller.hrmanager;

import com.group5.ems.service.hrmanager.HRAnalyticsService;
import com.group5.ems.service.hrmanager.HRManagerDashboardService;
import com.group5.ems.service.hrmanager.LeaveApprovalService;
import com.group5.ems.service.hrmanager.PayrollApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/hrmanager")
@RequiredArgsConstructor
public class HrManagerController {

    private final HRManagerDashboardService dashboardService;
    private final HRAnalyticsService analyticsService;
    private final LeaveApprovalService leaveApprovalService;  // ← thêm
    private final PayrollApprovalService payrollApprovalService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("kpi",              dashboardService.getKpiData());
        model.addAttribute("chartMonths",      dashboardService.getChartMonths());
        model.addAttribute("upcomingEvents",   dashboardService.getUpcomingEvents());
        model.addAttribute("recentActivities", dashboardService.getRecentActivities());
        model.addAttribute("activePage",       "dashboard");
        return "hrmanager/dashboard";
    }

    @GetMapping("/leave-approval")
    public String leaveApproval(Model model,
                                @RequestParam(defaultValue = "pending") String tab,
                                @RequestParam(defaultValue = "1") int page) {
        model.addAttribute("stats",         leaveApprovalService.getStats());
        model.addAttribute("leaveRequests", leaveApprovalService.getLeaveRequests(tab, page));
        model.addAttribute("pagination",    leaveApprovalService.getPagination(tab, page));
        model.addAttribute("activeTab",     tab);
        model.addAttribute("activePage",    "leave");
        return "hrmanager/leave_approval";
    }

    @GetMapping("/payroll-approval")
    public String payrollApproval(Model model,
                                  @RequestParam(defaultValue = "1") int page) {
        model.addAttribute("summary",     payrollApprovalService.getSummary());
        model.addAttribute("payrollRuns", payrollApprovalService.getPayrollRuns(page));
        model.addAttribute("pagination",  payrollApprovalService.getPagination(page));
        model.addAttribute("activePage",  "payroll");
        return "hrmanager/payroll_approval";
    }

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

    @PostMapping("/leave-approval/approve")
    public String approveLeaveRequest(@RequestParam Long requestId,
                                    @RequestParam Long approverId) {
        leaveApprovalService.approveLeaveRequest(requestId, approverId);
        return "redirect:/hrmanager/leave-approval?tab=pending";
    }

    @PostMapping("/leave-approval/reject")
    public String rejectLeaveRequest(@RequestParam Long requestId,
                                   @RequestParam Long approverId,
                                   @RequestParam String rejectedReason) {
        leaveApprovalService.rejectLeaveRequest(requestId, approverId, rejectedReason);
        return "redirect:/hrmanager/leave-approval?tab=pending";
    }
}