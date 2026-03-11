package com.group5.ems.controller.hr;

import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.service.hr.HrDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hr")
public class HrController {

    private final HrDashboardService dashboardService;
    private final com.group5.ems.service.hr.HrEmployeeService employeeService;
    private final com.group5.ems.service.hr.HrLeaveService leaveService;
    private final com.group5.ems.service.hr.HrPayrollService payrollService;
    private final com.group5.ems.service.hr.HrAttendanceService attendanceService;
    private final com.group5.ems.service.hr.HrRecruitmentService recruitmentService;
    private final com.group5.ems.service.hr.HrPerformanceService performanceService;
    private final com.group5.ems.service.hr.HrRequestService requestService;

    public HrController(HrDashboardService dashboardService, com.group5.ems.service.hr.HrEmployeeService employeeService, com.group5.ems.service.hr.HrLeaveService leaveService, com.group5.ems.service.hr.HrPayrollService payrollService, com.group5.ems.service.hr.HrAttendanceService attendanceService, com.group5.ems.service.hr.HrRecruitmentService recruitmentService, com.group5.ems.service.hr.HrPerformanceService performanceService, com.group5.ems.service.hr.HrRequestService requestService) {
        this.dashboardService = dashboardService;
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.payrollService = payrollService;
        this.attendanceService = attendanceService;
        this.recruitmentService = recruitmentService;
        this.performanceService = performanceService;
        this.requestService = requestService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        HrDashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();
        model.addAttribute("activeEmployees", metrics.activeEmployees());
        model.addAttribute("pendingLeave", metrics.pendingLeaveRequests());
        model.addAttribute("openJobs", metrics.openJobPosts());
        model.addAttribute("pendingRequests", metrics.pendingWorkflowRequests());
        return "hr/dashboard";
    }

    @GetMapping("/employees")
    public String employees(Model model) {
        model.addAttribute("employees", employeeService.getAllEmployees());
        return "hr/employees";
    }

    @GetMapping("/attendance")
    public String attendance(Model model) {
        model.addAttribute("attendances", attendanceService.getAllAttendances());
        return "hr/attendance";
    }

    @GetMapping("/leave")
    public String leave(Model model) {
        HrDashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();
        model.addAttribute("pendingLeave", metrics.pendingLeaveRequests());
        model.addAttribute("pendingRequests", metrics.pendingWorkflowRequests());
        
        model.addAttribute("pendingLeaves", leaveService.getPendingLeaves());
        model.addAttribute("leaveHistory", leaveService.getLeaveHistory());
        
        return "hr/leave";
    }

    @GetMapping("/payroll")
    public String payroll(Model model) {
        model.addAttribute("payslips", payrollService.getAllPayslips());
        return "hr/payroll";
    }

    @GetMapping("/recruitment")
    public String recruitment(Model model) {
        return "hr/recruitment";
    }

    @GetMapping("/performance")
    public String performance(Model model) {
        return "hr/performance";
    }

    @GetMapping("/requests")
    public String requests(Model model) {
        HrDashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();
        model.addAttribute("pendingLeave", metrics.pendingLeaveRequests());
        model.addAttribute("pendingRequests", metrics.pendingWorkflowRequests());
        return "hr/requests";
    }
}
