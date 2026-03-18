package com.group5.ems.controller.hr;

import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.dto.response.HrEmployeeDTO;
import com.group5.ems.dto.response.HrEmployeeDetailDTO;
import com.group5.ems.dto.response.HrLeaveRequestDTO;
import com.group5.ems.dto.response.HrPayrollSummaryDTO;
import com.group5.ems.dto.response.HrRequestDTO;
import com.group5.ems.entity.Department;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.service.hr.HrDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrController {

    private static final int PAGE_SIZE = 10;
    private static final int EMPLOYEE_PAGE_SIZE = 16;

    private final HrDashboardService dashboardService;
    private final com.group5.ems.service.hr.HrEmployeeService employeeService;
    private final com.group5.ems.service.hr.HrLeaveService leaveService;
    private final com.group5.ems.service.hr.HrPayrollService payrollService;
    private final com.group5.ems.service.hr.HrAttendanceService attendanceService;
    private final com.group5.ems.service.hr.HrRecruitmentService recruitmentService;
    private final com.group5.ems.service.hr.HrPerformanceService performanceService;
    private final com.group5.ems.service.hr.HrRequestService requestService;
    private final DepartmentRepository departmentRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        HrDashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();
        model.addAttribute("dashMetrics", metrics);
        model.addAttribute("activeEmployees", metrics.activeEmployees());
        model.addAttribute("pendingLeave", metrics.pendingLeaveRequests());
        model.addAttribute("openJobs", metrics.openJobPosts());
        model.addAttribute("pendingRequests", metrics.pendingWorkflowRequests());
        return "hr/dashboard";
    }

    @GetMapping("/employees")
    public String employees(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String department,
                            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, EMPLOYEE_PAGE_SIZE);
        Page<HrEmployeeDTO> employeePage = employeeService.searchEmployees(search, department, status, pageable);

        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", employeePage.getTotalPages());
        model.addAttribute("totalItems", employeePage.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("department", department);
        model.addAttribute("status", status);

        // Load departments for filter dropdown
        List<Department> departments = departmentRepository.findAll();
        model.addAttribute("departments", departments);

        return "hr/employees";
    }

    @GetMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<HrEmployeeDetailDTO> employeeDetail(@PathVariable Long id) {
        HrEmployeeDetailDTO detail = employeeService.getEmployeeDetail(id);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/attendance")
    public String attendance(Model model) {
        model.addAttribute("attendances", attendanceService.getAllAttendances());
        return "hr/attendance";
    }

    @GetMapping("/leave")
    public String leave(Model model,
                        @RequestParam(defaultValue = "0") int page) {
        HrDashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();
        model.addAttribute("pendingLeave", metrics.pendingLeaveRequests());
        model.addAttribute("pendingRequests", metrics.pendingWorkflowRequests());

        model.addAttribute("pendingLeaves", leaveService.getPendingLeaves());

        Pageable pageable = PageRequest.of(page, EMPLOYEE_PAGE_SIZE);
        Page<HrLeaveRequestDTO> historyPage = leaveService.getLeaveHistory(pageable);
        model.addAttribute("leaveHistory", historyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", historyPage.getTotalPages());
        model.addAttribute("totalItems", historyPage.getTotalElements());

        return "hr/leave";
    }

    @PostMapping("/leave/{id}/approve")
    public String approveLeave(@PathVariable Long id) {
        leaveService.approveLeave(id);
        return "redirect:/hr/leave";
    }

    @PostMapping("/leave/{id}/reject")
    public String rejectLeave(@PathVariable Long id, @RequestParam(required = false) String reason) {
        leaveService.rejectLeave(id, reason != null ? reason : "Rejected by HR");
        return "redirect:/hr/leave";
    }

    @GetMapping("/payroll")
    public String payroll(Model model) {
        model.addAttribute("payslips", payrollService.getAllPayslips());

        HrPayrollSummaryDTO summary = payrollService.getPayrollSummary();
        model.addAttribute("payrollSummary", summary);

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
    public String requests(Model model,
                           @RequestParam(defaultValue = "0") int page) {
        HrDashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();
        model.addAttribute("pendingLeave", metrics.pendingLeaveRequests());
        model.addAttribute("pendingRequests", metrics.pendingWorkflowRequests());

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<HrRequestDTO> requestPage = requestService.getAllWorkflowRequests(pageable);
        model.addAttribute("workflowRequests", requestPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", requestPage.getTotalPages());
        model.addAttribute("totalItems", requestPage.getTotalElements());

        return "hr/requests";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id) {
        requestService.approveRequest(id);
        return "redirect:/hr/requests";
    }

    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id, @RequestParam(required = false) String reason) {
        requestService.rejectRequest(id, reason != null ? reason : "Rejected by HR");
        return "redirect:/hr/requests";
    }
}
