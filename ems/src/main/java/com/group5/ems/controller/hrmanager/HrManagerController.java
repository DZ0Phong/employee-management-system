package com.group5.ems.controller.hrmanager;

import com.group5.ems.dto.response.hrmanager.EventCreateDTO;
import com.group5.ems.dto.response.hrmanager.EventUpdateDTO;
import com.group5.ems.service.hrmanager.CalendarService;
import com.group5.ems.service.hrmanager.HRAnalyticsService;
import com.group5.ems.service.hrmanager.HRManagerDashboardService;
import com.group5.ems.service.hrmanager.LeaveApprovalService;
import com.group5.ems.service.hrmanager.PayrollApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/hrmanager")
@RequiredArgsConstructor
public class HrManagerController {

    private final HRManagerDashboardService dashboardService;
    private final HRAnalyticsService analyticsService;
    private final LeaveApprovalService leaveApprovalService;
    private final PayrollApprovalService payrollApprovalService;
    private final CalendarService calendarService;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model,
                            @RequestParam(defaultValue = "all") String activityFilter) {
        model.addAttribute("kpi",                dashboardService.getKpiData());
        model.addAttribute("chartLabels",        dashboardService.getChartMonths());
        model.addAttribute("hiringData",         dashboardService.getHiringData());
        model.addAttribute("attritionData",      dashboardService.getAttritionData());
        model.addAttribute("upcomingEvents",     dashboardService.getUpcomingEvents());
        model.addAttribute("recentActivities",   dashboardService.getRecentActivities(activityFilter));
        model.addAttribute("activityCategories", dashboardService.getActivityCategories());
        model.addAttribute("activityFilter",     activityFilter);
        model.addAttribute("activePage",         "dashboard");
        return "hrmanager/dashboard";
    }

    // ── Dashboard Activities API (AJAX) ───────────────────────────────────────
    @GetMapping("/dashboard/activities")
    @ResponseBody
    public Map<String, Object> getActivities(@RequestParam(defaultValue = "all") String filter,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return dashboardService.getRecentActivitiesWithPagination(filter, page, size);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTIVITY CENTER - NEW ENDPOINTS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Get Quick Stats for Activity Center (4 cards)
     * Returns: leaveTotal, leavePending, payrollTotal, payrollPending, 
     *          statusChanges, hrTotal, hrPending, totalPending
     */
    @GetMapping("/dashboard/quick-stats")
    @ResponseBody
    public Map<String, Object> getQuickStats() {
        return dashboardService.getQuickStats();
    }

    /**
     * Get activities by type with filters
     * @param type: leave, payroll, status, hr, all
     * @param filter: pending, approved, all (for leave/hr), or specific sub-filters
     * @param days: number of days to look back (default 30)
     * @param limit: max number of items to return (default 10)
     */
    @GetMapping("/dashboard/activities/{type}")
    @ResponseBody
    public Map<String, Object> getActivitiesByType(
            @RequestParam(defaultValue = "leave") String type,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, Object> response = new HashMap<>();
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(days);
        java.time.LocalDate sinceDate = java.time.LocalDate.now().minusDays(days);
        
        try {
            java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> activities;
            
            switch (type.toLowerCase()) {
                case "leave":
                    activities = dashboardService.getLeaveActivities(since);
                    // Apply sub-filter if needed
                    if (!"all".equals(filter)) {
                        activities = filterLeaveActivities(activities, filter);
                    }
                    break;
                    
                case "payroll":
                    activities = dashboardService.getPayrollActivities(since);
                    // Apply sub-filter if needed
                    if (!"all".equals(filter)) {
                        activities = filterPayrollActivities(activities, filter);
                    }
                    break;
                    
                case "status":
                    activities = dashboardService.getStatusChangeActivities(sinceDate);
                    // Apply sub-filter if needed
                    if (!"all".equals(filter)) {
                        activities = filterStatusActivities(activities, filter);
                    }
                    break;
                    
                case "hr":
                    activities = dashboardService.getHRRequestActivities(since);
                    // Apply sub-filter if needed
                    if (!"all".equals(filter)) {
                        activities = filterHRActivities(activities, filter);
                    }
                    break;
                    
                case "all":
                default:
                    activities = dashboardService.getRecentActivitiesCombined(filter, days);
                    break;
            }
            
            // Limit to specified number of items
            int totalCount = activities.size();
            if (activities.size() > limit) {
                activities = activities.subList(0, limit);
            }
            
            response.put("success", true);
            response.put("activities", activities);
            response.put("count", activities.size());
            response.put("totalCount", totalCount);
            response.put("hasMore", totalCount > limit);
            response.put("type", type);
            response.put("filter", filter);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("activities", new java.util.ArrayList<>());
        }
        
        return response;
    }

    /**
     * Get combined activities for "All Activity" tab
     */
    @GetMapping("/dashboard/activities/combined")
    @ResponseBody
    public Map<String, Object> getCombinedActivities(
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> activities 
                    = dashboardService.getRecentActivitiesCombined(filter, days);
            
            // Limit to specified number of items
            int totalCount = activities.size();
            if (activities.size() > limit) {
                activities = activities.subList(0, limit);
            }
            
            response.put("success", true);
            response.put("activities", activities);
            response.put("count", activities.size());
            response.put("totalCount", totalCount);
            response.put("hasMore", totalCount > limit);
            response.put("filter", filter);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("activities", new java.util.ArrayList<>());
        }
        
        return response;
    }

    // ── Helper methods for filtering activities ──────────────────────────────

    private java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> filterLeaveActivities(
            java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> activities, 
            String filter) {
        
        return activities.stream()
                .filter(a -> {
                    switch (filter.toLowerCase()) {
                        case "pending":
                            return "PENDING".equals(a.getStatus());
                        case "approved":
                            return "APPROVED".equals(a.getStatus());
                        case "annual":
                            return a.getDetails() != null && a.getDetails().toLowerCase().contains("annual");
                        case "sick":
                            return a.getDetails() != null && a.getDetails().toLowerCase().contains("sick");
                        case "maternity":
                        case "paternity":
                            return a.getDetails() != null && 
                                   (a.getDetails().toLowerCase().contains("maternity") || 
                                    a.getDetails().toLowerCase().contains("paternity"));
                        case "unpaid":
                            return a.getDetails() != null && a.getDetails().toLowerCase().contains("unpaid");
                        default:
                            return true;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> filterPayrollActivities(
            java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> activities, 
            String filter) {
        
        return activities.stream()
                .filter(a -> {
                    switch (filter.toLowerCase()) {
                        case "pending":
                            return "PENDING".equals(a.getStatus());
                        case "approved":
                            return "APPROVED".equals(a.getStatus());
                        default:
                            return true;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> filterStatusActivities(
            java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> activities, 
            String filter) {
        
        return activities.stream()
                .filter(a -> {
                    switch (filter.toLowerCase()) {
                        case "newhire":
                        case "new_hire":
                            return "New hire".equalsIgnoreCase(a.getBadge());
                        case "termination":
                            return "Termination".equalsIgnoreCase(a.getBadge());
                        case "promotion":
                            return "Promotion".equalsIgnoreCase(a.getBadge());
                        case "transfer":
                            return "Transfer".equalsIgnoreCase(a.getBadge());
                        case "onleave":
                        case "on_leave":
                            return a.getDetails() != null && a.getDetails().toLowerCase().contains("on leave");
                        default:
                            return true;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> filterHRActivities(
            java.util.List<com.group5.ems.dto.response.hrmanager.RecentActivityDTO> activities, 
            String filter) {
        
        return activities.stream()
                .filter(a -> {
                    switch (filter.toLowerCase()) {
                        case "pending":
                            return "PENDING".equals(a.getStatus());
                        case "completed":
                            return "COMPLETED".equals(a.getStatus()) || "APPROVED".equals(a.getStatus());
                        case "contract":
                            return a.getActionLabel() != null && a.getActionLabel().toLowerCase().contains("contract");
                        case "benefits":
                            return a.getActionLabel() != null && a.getActionLabel().toLowerCase().contains("benefit");
                        case "documents":
                            return a.getActionLabel() != null && 
                                   (a.getActionLabel().toLowerCase().contains("document") ||
                                    a.getActionLabel().toLowerCase().contains("letter"));
                        default:
                            return true;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // END ACTIVITY CENTER ENDPOINTS
    // ══════════════════════════════════════════════════════════════════════════

    // ── Request Management (renamed from Leave Approval) ─────────────────────
    @GetMapping({"/leave-approval", "/request-management"})
    public String requestManagement(Model model,
                                @RequestParam(defaultValue = "pending") String tab,
                                @RequestParam(defaultValue = "all") String category,
                                @RequestParam(defaultValue = "1") int page) {
        model.addAttribute("stats",         leaveApprovalService.getStats());
        model.addAttribute("leaveRequests", leaveApprovalService.getLeaveRequests(tab, page));
        model.addAttribute("pagination",    leaveApprovalService.getPagination(tab, page));
        model.addAttribute("activeTab",     tab);
        model.addAttribute("activeCategory", category);
        model.addAttribute("activePage",    "leave");
        return "hrmanager/leave_approval";
    }

    // ── Payroll Approval ──────────────────────────────────────────────────────
    @GetMapping("/payroll-approval")
    public String payrollApproval(Model model,
                                  @RequestParam(defaultValue = "1") int page) {
        model.addAttribute("summary",     payrollApprovalService.getSummary());
        model.addAttribute("payrollRuns", payrollApprovalService.getPayrollRuns(page));
        model.addAttribute("pagination",  payrollApprovalService.getPagination(page));
        model.addAttribute("activePage",  "payroll");
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

    // ── Calendar ──────────────────────────────────────────────────────────────
    @GetMapping("/calendar")
    public String calendar(Model model,
                           @RequestParam(required = false) Integer month,
                           @RequestParam(required = false) Integer year) {
        LocalDate now = LocalDate.now();
        int currentMonth = month != null ? month : now.getMonthValue();
        int currentYear  = year  != null ? year  : now.getYear();

        model.addAttribute("events",       calendarService.getEventsByMonth(currentMonth, currentYear));
        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("currentYear",  currentYear);
        model.addAttribute("activePage",   "calendar");
        return "hrmanager/calendar";
    }

    // ── Calendar Create ───────────────────────────────────────────────────────
    @PostMapping("/calendar/create")
    public String createEvent(@ModelAttribute EventCreateDTO dto,
                              RedirectAttributes redirectAttributes) {
        try {
            calendarService.createEvent(dto, getCurrentUserId());
            redirectAttributes.addFlashAttribute("flashMessage", "Event created successfully!");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/calendar";
    }

    // ── Calendar Update ───────────────────────────────────────────────────────
    @PostMapping("/calendar/update")
    public String updateEvent(@ModelAttribute EventUpdateDTO dto,
                              RedirectAttributes redirectAttributes) {
        try {
            calendarService.updateEvent(dto, getCurrentUserId());
            redirectAttributes.addFlashAttribute("flashMessage", "Event updated successfully!");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/calendar";
    }

    // ── Calendar Delete ───────────────────────────────────────────────────────
    @PostMapping("/calendar/delete")
    public String deleteEvent(@RequestParam Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            calendarService.deleteEvent(id, getCurrentUserId());
            redirectAttributes.addFlashAttribute("flashMessage", "Event deleted successfully!");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/calendar";
    }

    // ── Request Management Actions (renamed from Leave Approval Actions) ─────
    @PostMapping({"/leave-approval/approve", "/request-management/approve"})
    public String approveRequest(@RequestParam Long requestId,
                                      @RequestParam Long approverId,
                                      RedirectAttributes redirectAttributes) {
        try {
            leaveApprovalService.approveLeaveRequest(requestId, approverId);
            redirectAttributes.addFlashAttribute("flashMessage", "Request approved successfully!");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", "Failed to approve: " + e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/leave-approval?tab=pending";
    }

    @PostMapping({"/leave-approval/reject", "/request-management/reject"})
    public String rejectRequest(@RequestParam Long requestId,
                                     @RequestParam Long approverId,
                                     @RequestParam String rejectedReason,
                                     RedirectAttributes redirectAttributes) {
        try {
            leaveApprovalService.rejectLeaveRequest(requestId, approverId, rejectedReason);
            redirectAttributes.addFlashAttribute("flashMessage", "Request rejected.");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", "Failed to reject: " + e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/leave-approval?tab=pending";
    }

    // ── Revert Request (24h window) - Phase 3 ────────────────────────────────
    @PostMapping({"/leave-approval/revert", "/request-management/revert"})
    public String revertRequest(@RequestParam Long requestId,
                                @RequestParam(required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            boolean success = leaveApprovalService.revertRequest(requestId, userId, reason);
            if (success) {
                redirectAttributes.addFlashAttribute("flashMessage", "Request reverted to pending status.");
                redirectAttributes.addFlashAttribute("flashType", "success");
            } else {
                redirectAttributes.addFlashAttribute("flashMessage", "Cannot revert: 24h window expired or not authorized.");
                redirectAttributes.addFlashAttribute("flashType", "error");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", "Failed to revert: " + e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/leave-approval";
    }

    // ── Payroll Approve by Department ─────────────────────────────────────────
    @PostMapping("/payroll-approval/approve")
    public String approvePayroll(@RequestParam Long deptId,
                                 RedirectAttributes redirectAttributes) {
        try {
            payrollApprovalService.approveByDepartment(deptId, getCurrentUserId());
            redirectAttributes.addFlashAttribute("flashMessage", "Payroll approved successfully!");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/payroll-approval";
    }

    @PostMapping("/payroll-approval/reject")
    public String rejectPayroll(@RequestParam Long deptId,
                                @RequestParam(required = false) String note,
                                RedirectAttributes redirectAttributes) {
        try {
            payrollApprovalService.rejectByDepartment(deptId, getCurrentUserId(), note);
            redirectAttributes.addFlashAttribute("flashMessage", "Payroll rejected.");
            redirectAttributes.addFlashAttribute("flashType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("flashType", "error");
        }
        return "redirect:/hrmanager/payroll-approval";
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private Long getCurrentUserId() {
        // TODO: thay bằng SecurityContext sau khi có Authentication
        return 1L;
    }
    
    // ========================================================================
    // ACTIVITY CENTER - APPROVE/REJECT ACTIONS
    // ========================================================================
    
    /**
     * Approve activity from Activity Center
     */
    @PostMapping("/dashboard/activity/approve")
    @ResponseBody
    public Map<String, Object> approveActivity(@RequestParam Long requestId) {
        Map<String, Object> response = new HashMap<>();
        try {
            leaveApprovalService.approveLeaveRequest(requestId, getCurrentUserId());
            response.put("success", true);
            response.put("message", "Request approved successfully!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to approve: " + e.getMessage());
        }
        return response;
    }
    
    /**
     * Reject activity from Activity Center
     */
    @PostMapping("/dashboard/activity/reject")
    @ResponseBody
    public Map<String, Object> rejectActivity(@RequestParam Long requestId,
                                              @RequestParam(required = false) String reason) {
        Map<String, Object> response = new HashMap<>();
        try {
            String rejectionReason = (reason != null && !reason.isEmpty()) 
                    ? reason 
                    : "Rejected by HR Manager";
            leaveApprovalService.rejectLeaveRequest(requestId, getCurrentUserId(), rejectionReason);
            response.put("success", true);
            response.put("message", "Request rejected successfully!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reject: " + e.getMessage());
        }
        return response;
    }
}