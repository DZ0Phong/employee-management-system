package com.group5.ems.controller.deptmanager;

import com.group5.ems.service.deptmanager.DeptManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group5.ems.service.deptmanager.AttendanceService;
import com.group5.ems.service.deptmanager.LeaveService;
import com.group5.ems.service.deptmanager.PerformanceService;

@Controller
@RequestMapping("/dept-manager")
@RequiredArgsConstructor
public class DeptManagerController {

    private final DeptManagerService deptManagerService;
    private final LeaveService leaveService;
    private final AttendanceService attendanceService;
    private final PerformanceService performanceService;

    @GetMapping({ "", "/", "/dashboard" })
    public String dashboard(Model model) {
        // Fetch mock data
        model.addAttribute("data", deptManagerService.getDashboardMockData());

        // Return the thymeleaf template
        return "deptmanager/dashboard";
    }

    @GetMapping("/my-team")
    public String team(Model model) {
        model.addAttribute("data", deptManagerService.getTeamMockData());
        return "deptmanager/team";
    }

    @GetMapping("/my-department")
    public String department(Model model) {
        model.addAttribute("data", deptManagerService.getDepartmentMockData());
        return "deptmanager/department";
    }

    @GetMapping("/leave-approval")
    public String leaveApproval(Model model) {
        model.addAttribute("data", leaveService.getLeaveApprovalData());
        return "deptmanager/leave-approval";
    }

    @GetMapping("/attendance-review")
    public String attendanceReview(@RequestParam(required = false, defaultValue = "0") int weekOffset, Model model) {
        model.addAttribute("data", attendanceService.getAttendanceReviewData(weekOffset));
        return "deptmanager/attendance-review";
    }

    @GetMapping("/performance-review")
    public String performanceReview(Model model) {
        model.addAttribute("data", performanceService.getPerformanceReviewData());
        return "deptmanager/performance-review";
    }

    @PostMapping("/leave-approval/{id}/approve")
    public String approveLeave(@PathVariable Long id) {
        leaveService.approveLeaveRequest(id);
        return "redirect:/dept-manager/leave-approval";
    }

    @PostMapping("/leave-approval/{id}/reject")
    public String rejectLeave(@PathVariable Long id) {
        leaveService.rejectLeaveRequest(id);
        return "redirect:/dept-manager/leave-approval";
    }

    @PostMapping("/team/request-removal")
    public String requestMemberRemoval(@RequestParam Long employeeId, @RequestParam String reason) {
        deptManagerService.createRemovalRequest(employeeId, reason);
        return "redirect:/dept-manager/my-team?success=removal";
    }

    @PostMapping("/team/request-add")
    public String requestAddMember(@RequestParam String requestType, @RequestParam String role, @RequestParam String description) {
        boolean success = deptManagerService.createAddMemberRequest(requestType, role, description);
        if(!success && "TRANSFER".equals(requestType)) {
            return "redirect:/dept-manager/my-team?error=notransfer";
        }
        return "redirect:/dept-manager/my-team?success=add";
    }
}
