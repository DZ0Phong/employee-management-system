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
import com.group5.ems.service.hrmanager.CalendarService;
import com.group5.ems.dto.request.hrmanager.EventCreateDTO;
import com.group5.ems.dto.request.hrmanager.EventUpdateDTO;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;


@Controller
@RequestMapping("/hrmanager")
@RequiredArgsConstructor
public class HrManagerController {

    private final HRManagerDashboardService dashboardService;
    private final HRAnalyticsService analyticsService;
    private final LeaveApprovalService leaveApprovalService;  // ← thêm
    private final PayrollApprovalService payrollApprovalService;
    private final CalendarService calendarService;


    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model,
                            @RequestParam(defaultValue = "all") String activityFilter) { // ✅ khai báo ở đây
        model.addAttribute("kpi",              dashboardService.getKpiData());
        model.addAttribute("chartLabels",      dashboardService.getChartMonths());
        model.addAttribute("hiringData",       dashboardService.getHiringData());
        model.addAttribute("attritionData",    dashboardService.getAttritionData());
        model.addAttribute("upcomingEvents",   dashboardService.getUpcomingEvents());
        model.addAttribute("activityFilter",   activityFilter); // ✅ dùng được rồi
        model.addAttribute("activePage",       "dashboard");
        model.addAttribute("recentActivities", dashboardService.getRecentActivities(activityFilter));        return "hrmanager/dashboard";
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

    // ── Calendar ──────────────────────────────────────────────────────────────
    @GetMapping("/calendar")
    public String calendar(Model model,
                           @RequestParam(required = false) Integer month,
                           @RequestParam(required = false) Integer year,
                           @RequestParam(defaultValue = "month") String view) {

        // Mặc định là tháng/năm hiện tại
        LocalDate now = LocalDate.now();
        int currentMonth = month != null ? month : now.getMonthValue();
        int currentYear  = year  != null ? year  : now.getYear();

        try {
            model.addAttribute("events",       eventService.getAllActiveEvents());
            model.addAttribute("weekEvents",   eventService.getUpcomingEventsForDashboard());
            model.addAttribute("categories",   eventCategoryService.getAllActiveCategories());
            model.addAttribute("currentMonth", currentMonth);
            model.addAttribute("currentYear",  currentYear);
            model.addAttribute("view",         view);
            model.addAttribute("activePage",   "calendar");
            return "hrmanager/calendar";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading calendar: " + e.getMessage());
            model.addAttribute("events", java.util.Collections.emptyList());
            model.addAttribute("weekEvents", java.util.Collections.emptyList());
            model.addAttribute("categories", java.util.Collections.emptyList());
            model.addAttribute("currentMonth", currentMonth);
            model.addAttribute("currentYear",  currentYear);
            model.addAttribute("view",         view);
            model.addAttribute("activePage",   "calendar");
            return "hrmanager/calendar";
        }
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

    // ── Thêm event ────────────────────────────────────────────────────────────
    @PostMapping("/calendar/create")
    public String createEvent(@ModelAttribute EventCreateDTO dto,
                              RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = getCurrentUserId(); // TODO: lấy từ SecurityContext
            calendarService.createEvent(dto, currentUserId);
            redirectAttributes.addFlashAttribute("flashMessage", "Event created successfully!");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/calendar";
    }

    // ── Sửa event ────────────────────────────────────────────────────────────
    @PostMapping("/calendar/update")
    public String updateEvent(@ModelAttribute EventUpdateDTO dto,
                              RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = getCurrentUserId();
            calendarService.updateEvent(dto, currentUserId);
            redirectAttributes.addFlashAttribute("flashMessage", "Event updated successfully!");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/calendar";
    }

    // ── Xóa event ────────────────────────────────────────────────────────────
    @PostMapping("/calendar/delete")
    public String deleteEvent(@RequestParam Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = getCurrentUserId();
            calendarService.deleteEvent(id, currentUserId);
            redirectAttributes.addFlashAttribute("flashMessage", "Event deleted successfully!");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/calendar";
    }

    // ── Helper lấy current user ───────────────────────────────────────────────
    private Long getCurrentUserId() {
        // TODO: thay bằng SecurityContext sau khi có Authentication
        return 1L; // tạm thời hard-code
    }

}