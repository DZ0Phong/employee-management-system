package com.group5.ems.controller.deptmanager;

import com.group5.ems.entity.Employee;
import com.group5.ems.entity.User;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.deptmanager.DeptManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

import com.group5.ems.service.deptmanager.AttendanceService;
import com.group5.ems.service.deptmanager.LeaveService;
import com.group5.ems.service.deptmanager.PerformanceService;

@Controller
@RequestMapping("/dept-manager")
@RequiredArgsConstructor
public class DeptManagerController {

    private static final String NO_DEPARTMENT_MESSAGE =
            "You have not been assigned to any department to manage yet.";

    private final DeptManagerService deptManagerService;
    private final LeaveService leaveService;
    private final AttendanceService attendanceService;
    private final PerformanceService performanceService;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    private User getUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Employee getEmployee(Authentication authentication) {
        User user = getUser(authentication);
        return employeeRepository.findByUserId(user.getId()).orElse(null);
    }

    private boolean hasDepartmentAccess(Authentication authentication) {
        Employee employee = getEmployee(authentication);
        return employee != null && employee.getDepartmentId() != null;
    }

    private String noDepartmentPage(Model model) {
        model.addAttribute("message", NO_DEPARTMENT_MESSAGE);
        model.addAttribute("employeePortalUrl", "/employee/dashboard");
        model.addAttribute("employeePortalLabel", "Employee View");
        return "common/no-department";
    }

    @GetMapping({ "", "/", "/dashboard" })
    public String dashboard(Model model, Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return noDepartmentPage(model);
        }
        model.addAttribute("data", deptManagerService.getDashboardData());
        return "deptmanager/dashboard";
    }

    @GetMapping("/my-team")
    public String team(Model model, Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return noDepartmentPage(model);
        }
        model.addAttribute("data", deptManagerService.getTeamData());
        return "deptmanager/team";
    }

    @GetMapping("/my-department")
    public String department(Model model, Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return noDepartmentPage(model);
        }
        model.addAttribute("data", deptManagerService.getDepartmentData());
        return "deptmanager/department";
    }

    @GetMapping("/leave-approval")
    public String leaveApproval(Model model, Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return noDepartmentPage(model);
        }
        model.addAttribute("data", leaveService.getLeaveApprovalData());
        return "deptmanager/leave-approval";
    }

    @GetMapping("/attendance-review")
    public String attendanceReview(@RequestParam(required = false, defaultValue = "0") int weekOffset,
                                   Model model,
                                   Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return noDepartmentPage(model);
        }
        model.addAttribute("data", attendanceService.getAttendanceReviewData(weekOffset));
        return "deptmanager/attendance-review";
    }

    @GetMapping("/performance-review")
    public String performanceReview(Model model, Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return noDepartmentPage(model);
        }
        model.addAttribute("data", performanceService.getPerformanceReviewData());
        return "deptmanager/performance-review";
    }

    @GetMapping("/performance-review/{id}")
    @ResponseBody
    public ResponseEntity<?> performanceReviewDetail(@PathVariable Long id, Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return ResponseEntity.status(403).body(NO_DEPARTMENT_MESSAGE);
        }
        try {
            return ResponseEntity.ok(performanceService.getReviewDetail(id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/performance-review/save")
    public String savePerformanceReview(@RequestParam(required = false) Long id,
                                        @RequestParam Long employeeId,
                                        @RequestParam String reviewPeriod,
                                        @RequestParam BigDecimal performanceScore,
                                        @RequestParam BigDecimal potentialScore,
                                        @RequestParam(required = false) String strengths,
                                        @RequestParam(required = false) String areasToImprove,
                                        @RequestParam(defaultValue = "DRAFT") String status,
                                        Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return "redirect:/dept-manager/dashboard";
        }
        try {
            performanceService.savePerformanceReview(
                    id,
                    employeeId,
                    reviewPeriod,
                    performanceScore,
                    potentialScore,
                    strengths,
                    areasToImprove,
                    status
            );
            return "redirect:/dept-manager/performance-review?success=saved";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return "redirect:/dept-manager/performance-review?error=save";
        }
    }

    @PostMapping("/leave-approval/{id}/approve")
    public String approveLeave(@PathVariable Long id,
                               @RequestParam(name = "urgent", defaultValue = "false") boolean urgent,
                               Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return "redirect:/dept-manager/dashboard";
        }
        boolean success = leaveService.approveLeaveRequest(id, urgent);
        return success
                ? "redirect:/dept-manager/leave-approval?success=forwarded"
                : "redirect:/dept-manager/leave-approval?error=forbidden";
    }

    @PostMapping("/leave-approval/{id}/reject")
    public String rejectLeave(@PathVariable Long id,
                              @RequestParam(name = "rejectionReason", required = false) String rejectionReason,
                              @RequestParam(name = "urgent", defaultValue = "false") boolean urgent,
                              Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return "redirect:/dept-manager/dashboard";
        }
        boolean success = leaveService.rejectLeaveRequest(id, rejectionReason, urgent);
        return success
                ? "redirect:/dept-manager/leave-approval?success=rejected"
                : "redirect:/dept-manager/leave-approval?error=forbidden";
    }

    @PostMapping("/team/request-removal")
    public String requestMemberRemoval(@RequestParam Long employeeId,
                                       @RequestParam String reason,
                                       Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return "redirect:/dept-manager/dashboard";
        }
        boolean success = deptManagerService.createRemovalRequest(employeeId, reason);
        return success
                ? "redirect:/dept-manager/my-department?success=removal"
                : "redirect:/dept-manager/my-department?error=invalidremoval";
    }

    @PostMapping("/team/request-add")
    public String requestAddMember(@RequestParam String requestType,
                                   @RequestParam String role,
                                   @RequestParam String description,
                                   Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return "redirect:/dept-manager/dashboard";
        }
        boolean success = deptManagerService.createAddMemberRequest(requestType, role, description);
        if (!success) {
            return "redirect:/dept-manager/my-department?error=addrequest";
        }
        return "redirect:/dept-manager/my-department?success=add";
    }

    @PostMapping("/my-department/staffing-updates/{id}/cancel")
    public String cancelStaffingUpdate(@PathVariable Long id,
                                       @RequestParam String sourceType,
                                       Authentication authentication) {
        if (!hasDepartmentAccess(authentication)) {
            return "redirect:/dept-manager/dashboard";
        }

        boolean success = "REMOVAL".equalsIgnoreCase(sourceType)
                ? deptManagerService.cancelRemovalRequest(id)
                : deptManagerService.cancelStaffingRequest(id);

        return success
                ? "redirect:/dept-manager/my-department?success=cancelled"
                : "redirect:/dept-manager/my-department?error=cancel";
    }
}
