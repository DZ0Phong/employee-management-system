package com.group5.ems.controller.hr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.dto.response.HrEmployeeDTO;
import com.group5.ems.dto.response.HrEmployeeDetailDTO;
import com.group5.ems.dto.response.HrLeaveRequestDTO;
import com.group5.ems.dto.response.HrPayrollSummaryDTO;
import com.group5.ems.dto.response.HrPerformanceDTO;
import com.group5.ems.dto.response.HrRequestDTO;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.PositionRepository;
import com.group5.ems.service.hr.HrAttendanceService;
import com.group5.ems.service.hr.HrDashboardService;
import com.group5.ems.service.hr.HrEmployeeService;
import com.group5.ems.service.hr.HrLeaveService;
import com.group5.ems.service.hr.HrPayrollService;
import com.group5.ems.service.hr.HrPerformanceService;
import com.group5.ems.service.hr.HrRecruitmentService;
import com.group5.ems.service.hr.HrRequestService;
import com.group5.ems.service.admin.AdminService;
import com.group5.ems.service.hr.HrBankDetailsService;
import com.group5.ems.entity.Employee;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrController {

    private static final int PAGE_SIZE = 10;
    private static final int EMPLOYEE_PAGE_SIZE = 16;

    private final HrDashboardService dashboardService;
    private final HrEmployeeService employeeService;
    private final HrLeaveService leaveService;
    private final HrPayrollService payrollService;
    private final HrAttendanceService attendanceService;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final JobPostRepository jobPostRepository;
    private final EmployeeRepository employeeRepository;
    private final HrRecruitmentService recruitmentService;
    private final HrPerformanceService performanceService;
    private final HrRequestService requestService;
    private final AdminService adminService;
    private final HrBankDetailsService bankDetailsService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        HrDashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();
        model.addAttribute("dashMetrics", metrics);
        model.addAttribute("activeEmployees", metrics.activeEmployees());
        model.addAttribute("pendingLeave", metrics.pendingLeaveRequests());
        model.addAttribute("openJobs", metrics.openJobPosts());
        model.addAttribute("pendingRequests", metrics.pendingWorkflowRequests());
        model.addAttribute("currentUser", adminService.getUserDTO().orElse(null));
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
        model.addAttribute("currentUser", adminService.getUserDTO().orElse(null));

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
        model.addAttribute("currentUser", adminService.getUserDTO().orElse(null));
        return "hr/attendance";
    }

    @GetMapping("/leave")
    public String leave(Model model,
                        @RequestParam(defaultValue = "0") int page) {
        HrDashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();
        model.addAttribute("pendingLeave", metrics.pendingLeaveRequests());
        model.addAttribute("pendingRequests", metrics.pendingWorkflowRequests());
        model.addAttribute("currentUser", adminService.getUserDTO().orElse(null));

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
        model.addAttribute("currentUser", adminService.getUserDTO().orElse(null));

        HrPayrollSummaryDTO summary = payrollService.getPayrollSummary();
        model.addAttribute("payrollSummary", summary);

        return "hr/payroll";
    }


    // ── Performance ────────────────────────────────────────────────────────

    @GetMapping("/performance")
    public String performance(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) Long departmentId,
                               @RequestParam(required = false) Long reviewerId,
                              @RequestParam(required = false) String reviewYear,
                              @RequestParam(required = false) String reviewPeriodType,
                              @RequestParam(required = false) BigDecimal minScore,
                              @RequestParam(required = false) BigDecimal maxScore,
                              @RequestParam(required = false) BigDecimal minPotential,
                              @RequestParam(required = false) BigDecimal maxPotential) {
        if (minScore != null && maxScore != null && minScore.compareTo(maxScore) > 0) {
            BigDecimal temp = minScore;
            minScore = maxScore;
            maxScore = temp;
        }
        if (minPotential != null && maxPotential != null && minPotential.compareTo(maxPotential) > 0) {
            BigDecimal temp = minPotential;
            minPotential = maxPotential;
            maxPotential = temp;
        }

        String reviewPeriod = null;
        if (reviewYear != null && !reviewYear.isEmpty()) {
            if (reviewPeriodType != null && !reviewPeriodType.isEmpty()) {
                reviewPeriod = reviewPeriodType.equals("Annual") ? "YEAR_" + reviewYear : reviewPeriodType + "_" + reviewYear;
            } else {
                reviewPeriod = "YEAR_" + reviewYear; 
            }
        }
        Pageable pageable = PageRequest.of(page, 12);
        Page<HrPerformanceDTO> reviewPage = performanceService.getReviews(search, status, departmentId, reviewerId, reviewPeriod, minScore, maxScore, minPotential, maxPotential, pageable);

        model.addAttribute("appraisals", reviewPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviewPage.getTotalPages());
        model.addAttribute("totalItems", reviewPage.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("reviewerId", reviewerId);
        model.addAttribute("reviewYear", reviewYear);
        model.addAttribute("reviewPeriodType", reviewPeriodType);
        model.addAttribute("minScore", minScore);
        model.addAttribute("maxScore", maxScore);
        model.addAttribute("minPotential", minPotential);
        model.addAttribute("maxPotential", maxPotential);
        model.addAttribute("currentUser", adminService.getUserDTO().orElse(null));
        // For filters & create modal
        model.addAttribute("employees", employeeRepository.findAllWithUser());
        model.addAttribute("reviewers", employeeRepository.findEmployeesByRoleCodes(List.of("HR", "HR_MANAGER")));
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("reviewPeriods", performanceService.getDistinctReviewPeriods());

        return "hr/performance";
    }

    @GetMapping("/performance/{id}")
    @ResponseBody
    public ResponseEntity<?> performanceDetail(@PathVariable Long id) {
        return performanceService.getReviewById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

    // ── Recruitment ────────────────────────────────────────────────────────

    @GetMapping("/recruitment")
    public String recruitment(Model model) {
        model.addAttribute("activeJobs",        recruitmentService.getActiveJobPosts());
        model.addAttribute("totalOpenJobs",     recruitmentService.countOpenJobs());
        model.addAttribute("recentApplicants",  recruitmentService.getRecentApplications());
        model.addAttribute("totalApplications", recruitmentService.countTotalApplications());
        model.addAttribute("departments",       departmentRepository.findAll());
        model.addAttribute("positions",         positionRepository.findAll());
        return "hr/recruitment";
    }

    /**
     * Create a new job post.
     * action  → status OPEN
     * action  → status DRAFT
     */
    @PostMapping("/recruitment/jobs/create")
    public String createJobPost(
            @RequestParam String title,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String requirements,
            @RequestParam(required = false) String benefits,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closeDate,
            @RequestParam(defaultValue = "draft") String action,
            RedirectAttributes redirectAttributes) {

        JobPost job = new JobPost();
        job.setTitle(title);
        job.setDepartmentId(departmentId);
        job.setPositionId(positionId);
        job.setDescription(description);
        job.setRequirements(requirements);
        job.setBenefits(benefits);
        job.setSalaryMin(salaryMin);
        job.setSalaryMax(salaryMax);
        job.setOpenDate(openDate != null ? openDate : LocalDate.now());
        job.setCloseDate(closeDate);
        job.setStatus("publish".equals(action) ? "OPEN" : "DRAFT");

        jobPostRepository.save(job);

        String msg = "publish".equals(action)
                ? "Job post \"" + title + "\" published successfully!"
                : "Job post \"" + title + "\" saved as draft.";
        redirectAttributes.addFlashAttribute("successMessage", msg);

        return "redirect:/hr/recruitment";
    }

    @PostMapping("/recruitment/applications/stage")
    public String updateApplicationStage(
            @RequestParam Long applicationId,
            @RequestParam String stage,
            @RequestParam(required = false, defaultValue = "") String note,
            RedirectAttributes redirectAttributes) {

        recruitmentService.updateApplicationStage(applicationId, stage, note);
        redirectAttributes.addFlashAttribute("successMessage",
                "Application moved to stage: " + stage);

        return "redirect:/hr/recruitment";
    }

    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id, @RequestParam(required = false) String reason) {
        requestService.rejectRequest(id, reason != null ? reason : "Rejected by HR");
        return "redirect:/hr/requests";
    }

    // ── Bank Details Management ──────────────────────────────────────────

    @GetMapping("/bank-details/{id}")
    public String employeeBankDetails(@PathVariable Long id,
                                      @RequestParam(defaultValue = "0") int page,
                                      Model model) {
        Employee employee = employeeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("employee", employee);
        model.addAttribute("bankDetails", bankDetailsService.getPaginatedMaskedBankDetails(id, pageable));
        model.addAttribute("currentUser", adminService.getUserDTO().orElse(null));
        return "hr/bank-details";
    }

    @PostMapping("/bank-details/{employeeId}/{bankId}/set-primary")
    public String setPrimaryBankDetail(@PathVariable Long employeeId,
                                       @PathVariable Long bankId,
                                       RedirectAttributes redirectAttributes) {
        try {
            bankDetailsService.setPrimaryAccount(employeeId, bankId);
            redirectAttributes.addFlashAttribute("successMessage", "Primary account updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/bank-details/" + employeeId;
    }

    @PostMapping("/bank-details/{employeeId}/{bankId}/delete")
    public String deleteBankDetail(@PathVariable Long employeeId,
                                   @PathVariable Long bankId,
                                   RedirectAttributes redirectAttributes) {
        try {
            bankDetailsService.deleteBankDetail(employeeId, bankId);
            redirectAttributes.addFlashAttribute("successMessage", "Bank detail deleted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/bank-details/" + employeeId;
    }
}
