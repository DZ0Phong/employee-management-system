package com.group5.ems.controller.hr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.ems.dto.request.BankDetailsFormDTO;
// import com.group5.ems.dto.request.EmployeeOnboardingCreateDTO;
import com.group5.ems.dto.response.ApplicationStageDTO;
import com.group5.ems.dto.response.CandidateCvDTO;
import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.dto.response.HrEmployeeDTO;
import com.group5.ems.dto.response.HrEmployeeDetailDTO;
import com.group5.ems.dto.response.HrLeaveRequestDTO;
// import com.group5.ems.dto.response.HrOnboardingTaskDTO;
// import com.group5.ems.dto.response.HrOnboardingTrackerDTO;
import com.group5.ems.dto.response.HrPerformanceDTO;
import com.group5.ems.dto.response.HrRecruitmentDTO;
import com.group5.ems.dto.response.InterviewerDTO;
import com.group5.ems.entity.CandidateCv;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.PositionRepository;
import com.group5.ems.repository.SkillRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.admin.AdminService;
import com.group5.ems.service.external.VietQrApiClient;
import com.group5.ems.service.hr.HrAttendanceService;
import com.group5.ems.service.hr.HrBankDetailsService;
import com.group5.ems.service.hr.HrDashboardService;
import com.group5.ems.service.hr.HrEmployeeService;
import com.group5.ems.service.hr.HrLeaveService;
// import com.group5.ems.service.hr.HrOnboardingService;
import com.group5.ems.service.hr.HrPayrollService;
import com.group5.ems.service.hr.HrPerformanceService;
import com.group5.ems.service.hr.HrRecruitmentService;
import com.group5.ems.service.hr.HrRequestService;
import com.group5.ems.service.hr.HrReportService;
import com.group5.ems.service.common.LogService;
import com.group5.ems.service.hr.HrCalendarService;
import com.group5.ems.dto.request.hr.HrEventCreateDTO;
import com.group5.ems.dto.request.hr.HrEventUpdateDTO;
import com.group5.ems.dto.response.hr.HrEventResponseDTO;
import com.group5.ems.dto.response.hr.HrEventDTO;
import com.group5.ems.exception.ReportExportException;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import jakarta.validation.Valid;
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
    private final UserRepository userRepository;
    private final HrRecruitmentService recruitmentService;
    private final HrPerformanceService performanceService;
    private final HrRequestService requestService;
    private final AdminService adminService;
    private final HrBankDetailsService bankDetailsService;
    private final VietQrApiClient vietQrApiClient;
    // private final HrOnboardingService onboardingService;
    private final HrReportService reportService;
    private final LogService logService;
    private final TemplateEngine templateEngine;
    private final SkillRepository skillRepository;
    private final HrCalendarService calendarService;

    @GetMapping({ "", "/", "/dashboard" })
    public String dashboard(Model model) {
        HrDashboardMetricsDTO metrics = dashboardService.getDashboardMetrics();
        model.addAttribute("dashMetrics", metrics);
        model.addAttribute("activeEmployees", metrics.activeEmployees());
        model.addAttribute("openJobs", metrics.openJobPosts());
        return "hr/dashboard";
    }

    @PostMapping("/dashboard/search")
    public String dashboardSearch(@RequestParam("code") String code, RedirectAttributes redirectAttributes) {
        Long empId = dashboardService.findEmployeeIdByCode(code.trim());
        if (empId != null) {
            return "redirect:/hr/employees?search=" + code.trim();
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Employee with code '" + code + "' not found.");
            return "redirect:/hr/dashboard";
        }
    }

    @GetMapping("/employees")
    public String employees(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int onboardingPage,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) Integer minProficiency,
            @RequestParam(defaultValue = "hireDate:desc") String sort,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String obSearch,
            @RequestParam(required = false) Long obDepartmentId) {

        // ── Directory Tab ─────────────────────────────
        String sortBy = "hireDate";
        String direction = "desc";
        if (sort != null && sort.contains(":")) {
            String[] parts = sort.split(":");
            sortBy = parts[0];
            direction = parts[1];
        }

        String sortField = switch (sortBy) {
            case "name" -> "user.fullName";
            case "proficiency" -> "proficiency";
            default -> "hireDate";
        };
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, EMPLOYEE_PAGE_SIZE, Sort.by(sortDirection, sortField));
        
        Page<HrEmployeeDTO> employeePage = employeeService.searchEmployees(search, department, status, skillId, minProficiency, pageable);

        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", employeePage.getTotalPages());
        model.addAttribute("totalItems", employeePage.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("department", department);
        model.addAttribute("status", status);
        model.addAttribute("skillId", skillId);
        model.addAttribute("minProficiency", minProficiency);
        model.addAttribute("sort", sort);

        model.addAttribute("skills", skillRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());

        // ── Onboarding Tracker Tab ────────────────────
        /*
        Pageable obPageable = PageRequest.of(onboardingPage, EMPLOYEE_PAGE_SIZE);
        Page<HrOnboardingTrackerDTO> onboardingTrackerPage = onboardingService.getOnboardingTracker(obSearch, obDepartmentId, obPageable);

        model.addAttribute("onboardingList", onboardingTrackerPage.getContent());
        model.addAttribute("obCurrentPage", onboardingPage);
        model.addAttribute("obTotalPages", onboardingTrackerPage.getTotalPages());
        model.addAttribute("obTotalItems", onboardingTrackerPage.getTotalElements());
        model.addAttribute("obSearch", obSearch);
        model.addAttribute("obDepartmentId", obDepartmentId);
        */

        // ── Wizard Form Data ──────────────────────────
        // model.addAttribute("onboardingTemplates", onboardingService.getActiveTemplates());

        model.addAttribute("activeTab", tab != null ? tab : "directory");

        return "hr/employees";
    }

    @GetMapping("/api/employees/search")
    @ResponseBody
    public ResponseEntity<List<HrEmployeeDTO>> searchEmployeesApi(@RequestParam String q) {
        Pageable pageable = PageRequest.of(0, 5);
        Page<HrEmployeeDTO> page = employeeService.searchEmployees(q, null, null, null, null, pageable);
        return ResponseEntity.ok(page.getContent());
    }

    @GetMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<HrEmployeeDetailDTO> employeeDetail(@PathVariable Long id) {
        HrEmployeeDetailDTO detail = employeeService.getEmployeeDetail(id);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/attendance")
    public String attendance(Model model,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page) {

        LocalDate queryDate = (date != null) ? date : LocalDate.now();

        model.addAttribute("stats", attendanceService.getAttendanceStats(queryDate));

        Pageable pageable = PageRequest.of(page, EMPLOYEE_PAGE_SIZE);
        org.springframework.data.domain.Page<com.group5.ems.dto.response.HrAttendanceDetailDTO> attendancePage = attendanceService
                .getAttendanceRecords(queryDate, search, departmentId, status, pageable);

        model.addAttribute("attendances", attendancePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", attendancePage.getTotalPages());
        model.addAttribute("totalItems", attendancePage.getTotalElements());

        model.addAttribute("date", queryDate);
        model.addAttribute("search", search);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("status", status);
        model.addAttribute("departments", departmentRepository.findAll());

        return "hr/attendance";
    }

    @GetMapping("/attendance/export")
    public void exportAttendaceCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        response.setContentType("text/csv");

        String filename = "attendance_export_" + queryDate + ".csv";

        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        attendanceService.exportAttendanceToCsv(queryDate, search, departmentId, status, response.getWriter());
    }

    @GetMapping("/leave")
    public String leave(Model model, jakarta.servlet.http.HttpSession session,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String clear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int balancePage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String leaveType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String statsMonth) {

        if (tab == null) {
            tab = (String) session.getAttribute("leave_activeTab");
            if (tab == null) tab = "current";
        } else {
            session.setAttribute("leave_activeTab", tab);
        }

        boolean hasFilterParams = (status != null || departmentId != null || leaveType != null || search != null || dateFrom != null || dateTo != null || statsMonth != null);

        if ("true".equals(clear)) {
            session.removeAttribute("leave_" + tab + "_status");
            session.removeAttribute("leave_" + tab + "_departmentId");
            session.removeAttribute("leave_" + tab + "_leaveType");
            session.removeAttribute("leave_" + tab + "_search");
            session.removeAttribute("leave_" + tab + "_dateFrom");
            session.removeAttribute("leave_" + tab + "_dateTo");
            // DO NOT clear statsMonth here so stats filter persists across search clears
        } else if (hasFilterParams) {
            session.setAttribute("leave_" + tab + "_status", status);
            session.setAttribute("leave_" + tab + "_departmentId", departmentId);
            session.setAttribute("leave_" + tab + "_leaveType", leaveType);
            session.setAttribute("leave_" + tab + "_search", search);
            session.setAttribute("leave_" + tab + "_dateFrom", dateFrom);
            session.setAttribute("leave_" + tab + "_dateTo", dateTo);
            session.setAttribute("leave_" + tab + "_statsMonth", statsMonth);
        }

        // Current tab
        String curSearch = (String) session.getAttribute("leave_current_search");
        Long curDept = (Long) session.getAttribute("leave_current_departmentId");
        String curType = (String) session.getAttribute("leave_current_leaveType");
        model.addAttribute("pendingLeaves", leaveService.getPendingLeaves(curSearch, curDept, curType));
        model.addAttribute("currentSearch", curSearch);
        model.addAttribute("currentDepartmentId", curDept);
        model.addAttribute("currentLeaveType", curType);

        // HRM tab
        String hrmSearch = (String) session.getAttribute("leave_hrm_search");
        Long hrmDept = (Long) session.getAttribute("leave_hrm_departmentId");
        String hrmType = (String) session.getAttribute("leave_hrm_leaveType");
        model.addAttribute("hrmPendingLeaves", leaveService.getHrmPendingLeaves(hrmSearch, hrmDept, hrmType));
        model.addAttribute("hrmSearch", hrmSearch);
        model.addAttribute("hrmDepartmentId", hrmDept);
        model.addAttribute("hrmLeaveType", hrmType);

        // History tab
        String histStatus = (String) session.getAttribute("leave_history_status");
        String histSearch = (String) session.getAttribute("leave_history_search");
        Long histDept = (Long) session.getAttribute("leave_history_departmentId");
        String histType = (String) session.getAttribute("leave_history_leaveType");
        LocalDate histFrom = (LocalDate) session.getAttribute("leave_history_dateFrom");
        LocalDate histTo = (LocalDate) session.getAttribute("leave_history_dateTo");
        
        Pageable pageable = PageRequest.of(page, EMPLOYEE_PAGE_SIZE);
        Page<HrLeaveRequestDTO> historyPage = leaveService.getLeaveHistoryFiltered(histStatus, histDept, histType, histSearch, histFrom, histTo, pageable);
        model.addAttribute("leaveHistory", historyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", historyPage.getTotalPages());
        model.addAttribute("totalItems", historyPage.getTotalElements());
        model.addAttribute("histSearch", histSearch);
        model.addAttribute("histDepartmentId", histDept);
        model.addAttribute("histLeaveType", histType);
        model.addAttribute("histStatus", histStatus);
        model.addAttribute("histDateFrom", histFrom);
        model.addAttribute("histDateTo", histTo);

        // Stats tab
        String sMonth = (String) session.getAttribute("leave_stats_statsMonth");
        String statsSearch = (String) session.getAttribute("leave_stats_search");
        Long statsDept = (Long) session.getAttribute("leave_stats_departmentId");
        model.addAttribute("leaveStats", leaveService.getLeaveStats(sMonth));
        model.addAttribute("statsSearch", statsSearch);
        model.addAttribute("statsDepartmentId", statsDept);
        model.addAttribute("statsMonth", sMonth);
        
        boolean isCurrentMonth = true;
        if (sMonth != null && !sMonth.trim().isEmpty()) {
            try {
                java.time.YearMonth requestedMonth = java.time.YearMonth.parse(sMonth);
                isCurrentMonth = requestedMonth.equals(java.time.YearMonth.now());
                model.addAttribute("statsMonthStart", requestedMonth.atDay(1));
                model.addAttribute("statsMonthEnd", requestedMonth.atEndOfMonth());
            } catch (Exception e) {}
        }
        model.addAttribute("isCurrentMonth", isCurrentMonth);

        // Balance Table
        Pageable balancePageable = PageRequest.of(balancePage, EMPLOYEE_PAGE_SIZE);
        Page<com.group5.ems.dto.response.HrEmployeeLeaveBalanceDTO> balances = leaveService.getLeaveBalancesFiltered(statsDept, statsSearch, balancePageable);
        model.addAttribute("leaveBalances", balances.getContent());
        model.addAttribute("balanceCurrentPage", balancePage);
        model.addAttribute("balanceTotalPages", balances.getTotalPages());
        model.addAttribute("balanceTotalItems", balances.getTotalElements());

        // Calendar Tab (filters are handled client-side but we preserve them here if needed, or pass normal variables if they render immediately)
        model.addAttribute("balanceSummary", leaveService.getLeaveBalanceSummary());
        model.addAttribute("rejectionCategories", leaveService.getRejectionCategories());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("activeTab", tab);

        return "hr/leave";
    }

    @PostMapping("/leave/{id}/approve")
    public String approveLeave(@PathVariable Long id, RedirectAttributes ra) {
        try {
            leaveService.approveLeave(id);
            ra.addFlashAttribute("successMessage", "Leave request approved successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/leave";
    }

    @PostMapping("/leave/{id}/reject")
    public String rejectLeave(@PathVariable Long id,
            @RequestParam(required = false) String category,
            @RequestParam String reason,
            RedirectAttributes ra) {
        try {
            leaveService.rejectLeave(id, category, reason);
            ra.addFlashAttribute("successMessage", "Leave request rejected.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/leave";
    }

    // ── Bulk operations (improvement #5) ──

    @PostMapping("/leave/bulk-approve")
    public String bulkApproveLeave(@RequestParam List<Long> ids, RedirectAttributes ra) {
        try {
            int count = leaveService.bulkApprove(ids);
            ra.addFlashAttribute("successMessage", count + " leave request(s) approved.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/leave";
    }

    @PostMapping("/leave/bulk-reject")
    public String bulkRejectLeave(
            @RequestParam List<Long> ids,
            @RequestParam(required = false) String category,
            @RequestParam String reason,
            RedirectAttributes ra) {
        try {
            int count = leaveService.bulkReject(ids, category, reason);
            ra.addFlashAttribute("successMessage", count + " leave request(s) rejected.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/leave";
    }

    // ── Calendar API (improvement #3) ──

    @GetMapping("/leave/calendar")
    @ResponseBody
    public ResponseEntity<List<com.group5.ems.dto.response.HrLeaveCalendarEventDTO>> leaveCalendarEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String leaveType,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(leaveService.getCalendarEvents(start, end, departmentId, leaveType, search));
    }

    // ── CSV export (improvement #7) ──

    @GetMapping("/leave/export")
    public void exportLeaveCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String leaveType,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        
        String filename = "leave-history";
        if (departmentId != null) {
            Department d = departmentRepository.findById(departmentId).orElse(null);
            if (d != null) {
                filename += "-dept-" + d.getName().replaceAll("[^a-zA-Z0-9-]", "_");
            } else {
                filename += "-dept-" + departmentId;
            }
        }
        if (leaveType != null && !leaveType.isEmpty()) filename += "-type-" + leaveType.toLowerCase();
        if (startDate != null) filename += "-from-" + startDate;
        if (endDate != null) filename += "-to-" + endDate;
        filename += ".csv";
        
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        leaveService.exportLeaveHistoryToCsv(status, departmentId, leaveType, startDate, endDate, response.getWriter());
    }

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
                reviewPeriod = reviewPeriodType.equals("Annual")
                        ? "YEAR_" + reviewYear
                        : reviewPeriodType + "_" + reviewYear;
            } else {
                reviewPeriod = "YEAR_" + reviewYear;
            }
        }

        Pageable pageable = PageRequest.of(page, 12);
        Page<HrPerformanceDTO> reviewPage = performanceService.getReviews(
                search, status, departmentId, reviewerId,
                reviewPeriod, minScore, maxScore, minPotential, maxPotential, pageable);

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        // Tab 1: Pending requests
        model.addAttribute("pendingList", requestService.getPendingRequests());

        // Tab: HRM Pending (read only)
        model.addAttribute("hrmPendingList", requestService.getHrmPendingRequests());

        // Tab 2: Filtered history
        java.time.LocalDateTime dtFrom = dateFrom != null ? dateFrom.atStartOfDay() : null;
        java.time.LocalDateTime dtTo = dateTo != null ? dateTo.atTime(23, 59, 59) : null;

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        var historyPage = requestService.getRequestHistoryFiltered(
                status, categoryCode, search, dtFrom, dtTo, pageable);
        model.addAttribute("historyList", historyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", historyPage.getTotalPages());
        model.addAttribute("totalItems", historyPage.getTotalElements());

        // Tab 3: Create request form data
        model.addAttribute("requestTypes", requestService.getCreatableRequestTypes());

        // Tab 4: Analytics
        model.addAttribute("stats", requestService.getRequestStats());

        // Rejection modal data
        model.addAttribute("rejectionCategories", requestService.getRejectionCategories());

        // Preserve filter values
        model.addAttribute("activeTab", tab != null ? tab : "pending");
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterCategory", categoryCode);
        model.addAttribute("filterSearch", search);
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);

        return "hr/requests";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            requestService.approveRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "Request #" + id + " approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/requests";
    }

    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {
        try {
            requestService.rejectRequest(id, category, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Request #" + id + " rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/requests";
    }

    @PostMapping("/requests/create")
    public String createRequest(
            @RequestParam Long requestTypeId,
            @RequestParam String title,
            @RequestParam String content,
            RedirectAttributes redirectAttributes) {
        try {
            requestService.createRequest(requestTypeId, title, content);
            redirectAttributes.addFlashAttribute("successMessage", "Request created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/requests?tab=pending";
    }

    @PostMapping("/requests/bulk-approve")
    public String bulkApproveRequests(@RequestParam List<Long> ids,
            RedirectAttributes redirectAttributes) {
        try {
            int count = requestService.bulkApprove(ids);
            redirectAttributes.addFlashAttribute("successMessage", count + " request(s) approved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/requests";
    }

    @PostMapping("/requests/bulk-reject")
    public String bulkRejectRequests(@RequestParam List<Long> ids,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {
        try {
            int count = requestService.bulkReject(ids, category, reason);
            redirectAttributes.addFlashAttribute("successMessage", count + " request(s) rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/requests";
    }

    @GetMapping("/recruitment")
    public String recruitment(Model model,
            @AuthenticationPrincipal UserDetails principal) {
        model.addAttribute("activeJobs", recruitmentService.getActiveJobPosts());
        model.addAttribute("totalOpenJobs", recruitmentService.countOpenJobs());
        model.addAttribute("recentApplicants", recruitmentService.getRecentApplications());
        model.addAttribute("totalApplications", recruitmentService.countTotalApplications());
        model.addAttribute("interviewers", recruitmentService.getAvailableInterviewers());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("positions", positionRepository.findAll());
        model.addAttribute("jobPostRequests", recruitmentService.getJobPostRequests());
        model.addAttribute("pendingJobRequests", recruitmentService.countPendingJobRequests());

        // My Interviews tab — lấy theo user đang đăng nhập
        Long currentUserId = resolveCurrentUserId(principal);
        model.addAttribute("currentUserId", currentUserId);
        java.util.List<com.group5.ems.dto.response.InterviewDTO> myInterviews = recruitmentService
                .getMyInterviews(currentUserId);
        model.addAttribute("myInterviews", myInterviews);
        model.addAttribute("myInterviewScheduled",
                myInterviews.stream().filter(i -> "SCHEDULED".equals(i.getStatus())).count());

        return "hr/recruitment";
    }

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
            @RequestParam(required = false) Long fromRequestId,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {
        recruitmentService.validateCreateJobPost(
                openDate != null ? openDate : LocalDate.now(), closeDate, action);

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

        if (fromRequestId != null && "publish".equals(action)) {
            recruitmentService.approveJobRequest(fromRequestId, resolveCurrentUserId(principal));
        }

        ra.addFlashAttribute("successMessage",
                "publish".equals(action)
                        ? "Job post \"" + title + "\" published successfully!"
                        : "Job post \"" + title + "\" saved as draft.");
        return "redirect:/hr/recruitment";
    }

    @PostMapping("/recruitment/applications/stage")
    public String updateApplicationStage(
            @RequestParam Long applicationId,
            @RequestParam String stage,
            @RequestParam(required = false, defaultValue = "") String note,
            RedirectAttributes ra) {

        recruitmentService.updateApplicationStage(applicationId, stage, note);
        ra.addFlashAttribute("successMessage", "Application moved to stage: " + stage);
        return "redirect:/hr/recruitment";
    }

    @GetMapping("/recruitment/applications/{id}/stages")
    @ResponseBody
    public ResponseEntity<List<ApplicationStageDTO>> getApplicationStages(
            @PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getStageHistory(id));
    }

    @GetMapping("/recruitment/applications/{id}/interviewers")
    @ResponseBody
    public ResponseEntity<List<InterviewerDTO>> getAssignedInterviewers(
            @PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getAssignedInterviewers(id));
    }

    @PostMapping("/recruitment/applications/assign")
    public String assignInterviewers(
            @RequestParam Long applicationId,
            @RequestParam(required = false) List<Long> interviewerIds,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {

        recruitmentService.assignInterviewers(
                applicationId, interviewerIds, resolveCurrentUserId(principal));
        ra.addFlashAttribute("successMessage", "Interviewers assigned successfully.");
        return "redirect:/hr/recruitment";
    }

    @GetMapping("/recruitment/candidates/{candidateId}/cvs")
    @ResponseBody
    public ResponseEntity<List<CandidateCvDTO>> getCandidateCvs(
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(recruitmentService.getCvMetadata(candidateId));
    }

    @GetMapping("/recruitment/cvs/{cvId}/view")
    @ResponseBody
    public ResponseEntity<byte[]> viewCv(@PathVariable Long cvId) {
        CandidateCv cv = recruitmentService.getCvBlob(cvId);
        byte[] data = cv.getFileData();
        MediaType mt = recruitmentService.resolveMediaType(cv.getFileType());

        return ResponseEntity.ok()
                .contentType(mt)
                .contentLength(data.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + cv.getFileName() + "\"")
                .body(data);
    }

    @GetMapping("/recruitment/cvs/{cvId}/download")
    public ResponseEntity<byte[]> downloadCv(@PathVariable Long cvId) {
        CandidateCv cv = recruitmentService.getCvBlob(cvId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(cv.getFileData().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + cv.getFileName() + "\"")
                .body(cv.getFileData());
    }

    @PostMapping("/recruitment/candidates/{candidateId}/cvs/upload")
    @ResponseBody
    public ResponseEntity<CandidateCvDTO> uploadCv(
            @PathVariable Long candidateId,
            @RequestParam("file") MultipartFile file) {

        CandidateCv saved = recruitmentService.uploadCv(candidateId, file);
        return ResponseEntity.ok(new CandidateCvDTO(
                saved.getId(),
                saved.getFileName(),
                saved.getFileType(),
                saved.getUploadedAt() != null
                        ? saved.getUploadedAt()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : ""));
    }

    @DeleteMapping("/recruitment/cvs/{cvId}")

    @PostMapping("/recruitment/requests/{id}/approve")
    public String approveJobRequest(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {
        recruitmentService.approveJobRequest(id, resolveCurrentUserId(principal));
        ra.addFlashAttribute("successMessage", "Job request approved successfully.");
        return "redirect:/hr/recruitment";
    }

    @PostMapping("/recruitment/requests/{id}/reject")
    public String rejectJobRequest(@PathVariable Long id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {
        recruitmentService.rejectJobRequest(id,
                reason != null ? reason : "Rejected by HR",
                resolveCurrentUserId(principal));
        ra.addFlashAttribute("successMessage", "Job request rejected.");
        return "redirect:/hr/recruitment";
    }

    @ResponseBody
    public ResponseEntity<Void> deleteCv(@PathVariable Long cvId) {
        recruitmentService.deleteCv(cvId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recruitment/jobs")
    public String allJobs(Model model) {
        List<HrRecruitmentDTO> allJobs = recruitmentService.getAllJobPosts();
        model.addAttribute("allJobs", allJobs);
        model.addAttribute("openCount", allJobs.stream().filter(j -> "OPEN".equals(j.getStatus())).count());
        model.addAttribute("draftCount", allJobs.stream().filter(j -> "DRAFT".equals(j.getStatus())).count());
        model.addAttribute("closedCount", allJobs.stream().filter(j -> "CLOSED".equals(j.getStatus())).count());
        model.addAttribute("totalApplicants", allJobs.stream().mapToLong(HrRecruitmentDTO::getApplicantCount).sum());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("positions", positionRepository.findAll());
        return "hr/recruitment-jobs";
    }

    @PostMapping("/recruitment/jobs/update")
    public String updateJobPost(
            @RequestParam Long id,
            @RequestParam String title,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String requirements,
            @RequestParam(required = false) String benefits,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closeDate,
            @RequestParam(required = false) String status,
            RedirectAttributes ra) {
        recruitmentService.validateUpdateJobPost(id, openDate, closeDate, status);

        recruitmentService.updateJobPost(id, title, departmentId, description,
                requirements, benefits, salaryMin, salaryMax, openDate, closeDate, status);
        ra.addFlashAttribute("successMessage", "Job post \"" + title + "\" updated successfully!");
        return "redirect:/hr/recruitment/jobs";
    }

    @PostMapping("/recruitment/jobs/delete")
    public String deleteJobPost(
            @RequestParam Long id,
            RedirectAttributes ra) {
        recruitmentService.validateDeleteJobPost(id);

        String title = recruitmentService.deleteJobPost(id);
        ra.addFlashAttribute("successMessage", "Job post \"" + title + "\" deleted successfully.");
        return "redirect:/hr/recruitment/jobs";
    }

    // ── GET: My Interviews page ────────────────────────────────────────────

    @GetMapping("/recruitment/my-interviews")
    public String myInterviews(Model model,
            @AuthenticationPrincipal UserDetails principal) {
        Long currentUserId = resolveCurrentUserId(principal);
        model.addAttribute("myInterviews", recruitmentService.getMyInterviews(currentUserId));
        return "hr/my-interviews";
    }

    // ── GET: Interviews của 1 application ───────

    @GetMapping("/recruitment/applications/{id}/interviews")
    @ResponseBody
    public ResponseEntity<List<com.group5.ems.dto.response.InterviewDTO>> getApplicationInterviews(
            @PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getInterviewsByApplication(id));
    }

    // ── POST: Schedule interview ────────────────────────────────────────────

    @PostMapping("/recruitment/interviews/schedule")
    public String scheduleInterview(
            @RequestParam Long applicationId,
            @RequestParam Long interviewerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime scheduledAt,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, defaultValue = "/hr/recruitment") String returnUrl,
            RedirectAttributes ra) {
        try {
            recruitmentService.scheduleInterview(applicationId, interviewerId, scheduledAt, location);
            ra.addFlashAttribute("successMessage", "Interview scheduled successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + returnUrl;
    }

    // ── POST: Submit feedback ───────────────────────────────────────────────

    @PostMapping("/recruitment/interviews/{id}/feedback")
    public String submitFeedback(
            @PathVariable Long id,
            @RequestParam String feedback,
            @RequestParam String status,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {
        try {
            recruitmentService.submitFeedback(id, feedback, status);
            ra.addFlashAttribute("successMessage", "Feedback submitted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/recruitment/my-interviews";
    }

    // Overload: redirect về recruitment nếu HR submit từ candidate modal
    @PostMapping("/recruitment/interviews/{id}/feedback-hr")
    public String submitFeedbackFromHr(
            @PathVariable Long id,
            @RequestParam String feedback,
            @RequestParam String status,
            RedirectAttributes ra) {
        try {
            recruitmentService.submitFeedback(id, feedback, status);
            ra.addFlashAttribute("successMessage", "Feedback updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/recruitment";
    }

    // ── POST: Cancel interview ──────────────────────────────────────────────

    @PostMapping("/recruitment/interviews/{id}/cancel")
    public String cancelInterview(
            @PathVariable Long id,
            @RequestParam(defaultValue = "/hr/recruitment") String returnUrl,
            RedirectAttributes ra) {
        try {
            recruitmentService.cancelInterview(id);
            ra.addFlashAttribute("successMessage", "Interview cancelled.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + returnUrl;
    }

    private Long resolveCurrentUserId(UserDetails principal) {
        if (principal == null)
            return null;
        return userRepository.findByUsername(principal.getUsername())
                .map(user -> user.getId())
                .orElse(null);
    }

    // ── Bank Details Management ──────────────────────────────────────────

    @GetMapping("/my-profile/bank-details")
    public String myBankDetailsRedirect() {
        Employee employee = adminService.getCurrentUser()
                .map(com.group5.ems.entity.User::getEmployee)
                .orElseThrow(() -> new RuntimeException("Logged in HR user has no employee record"));
        return "redirect:/hr/bank-details/" + employee.getId();
    }

    @GetMapping("/bank-details/{id}")
    public String employeeBankDetails(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Employee employee = employeeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<com.group5.ems.dto.response.BankDetailsResponseDTO> historyPage = bankDetailsService
                .getBankDetailsHistory(id, pageable);

        model.addAttribute("employee", employee);
        model.addAttribute("bankDetails", historyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", historyPage.getTotalPages());
        model.addAttribute("totalItems", historyPage.getTotalElements());
        model.addAttribute("banks", vietQrApiClient.getSupportedBanks());

        if (!model.containsAttribute("bankDetailsForm")) {
            model.addAttribute("bankDetailsForm", new BankDetailsFormDTO());
        }

        model.addAttribute("currentUser", adminService.getUserDTO().orElse(null));
        return "hr/bank-details";
    }

    @PostMapping("/bank-details/{id}/add")
    public String addBankDetails(@PathVariable Long id,
            @Valid @ModelAttribute("bankDetailsForm") BankDetailsFormDTO form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (result.hasErrors()) {
            Employee employee = employeeRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            model.addAttribute("employee", employee);

            Pageable pageable = PageRequest.of(0, PAGE_SIZE);
            Page<com.group5.ems.dto.response.BankDetailsResponseDTO> historyPage = bankDetailsService
                    .getBankDetailsHistory(id, pageable);

            model.addAttribute("bankDetails", historyPage.getContent());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", historyPage.getTotalPages());
            model.addAttribute("totalItems", historyPage.getTotalElements());

            model.addAttribute("banks", vietQrApiClient.getSupportedBanks());
            return "hr/bank-details";
        }

        try {
            bankDetailsService.addBankDetails(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Bank details added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/bank-details/" + id;
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

    // ── Onboarding Endpoints ──────────────────────────────────────────

/*
    @PostMapping("/employees/onboard")
    public String createEmployeeOnboard(
            @Valid @ModelAttribute EmployeeOnboardingCreateDTO form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (result.hasErrors()) {
            StringBuilder errors = new StringBuilder();
            result.getAllErrors().forEach(e -> errors.append(e.getDefaultMessage()).append(". "));
            redirectAttributes.addFlashAttribute("errorMessage", errors.toString());
            return "redirect:/hr/employees?tab=directory";
        }
        try {
            Long empId = onboardingService.createEmployeeAndStartOnboarding(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Employee created successfully (Code: EMP-" + String.format("%04d", empId) + "). " +
                    "A preboarding email has been queued for " + form.getEmail() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/employees?tab=onboarding";
    }

    @GetMapping("/onboarding/{onboardingId}/tasks")
    @ResponseBody
    public ResponseEntity<List<HrOnboardingTaskDTO>> getOnboardingTasks(@PathVariable Long onboardingId) {
        return ResponseEntity.ok(onboardingService.getOnboardingTasks(onboardingId));
    }

    @PostMapping("/onboarding/tasks/{taskId}/complete")
    public String completeOnboardingTask(@PathVariable Long taskId,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes) {
        try {
            onboardingService.completeTask(taskId, resolveCurrentUserId(principal));
            redirectAttributes.addFlashAttribute("successMessage", "Task status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/employees?tab=onboarding";
    }

    @PostMapping("/onboarding/tasks/{taskId}/skip")
    public String skipOnboardingTask(@PathVariable Long taskId,
            RedirectAttributes redirectAttributes) {
        try {
            onboardingService.skipTask(taskId);
            redirectAttributes.addFlashAttribute("successMessage", "Task skipped.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/employees?tab=onboarding";
    }

    @PostMapping("/onboarding/{onboardingId}/cancel")
    public String cancelOnboarding(@PathVariable Long onboardingId,
            RedirectAttributes redirectAttributes) {
        try {
            onboardingService.cancelOnboarding(onboardingId);
            redirectAttributes.addFlashAttribute("successMessage", "Onboarding cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/hr/employees?tab=onboarding";
    }

    @GetMapping("/api/onboarding/templates")
    @ResponseBody
    public ResponseEntity<List<com.group5.ems.dto.response.HrOnboardingTemplateDTO>> getTemplatesForDepartment(
            @RequestParam Long departmentId) {
        return ResponseEntity.ok(onboardingService.getTemplatesForDepartment(departmentId));
    }
*/

    // ═══════════════════════════════════════════════════════════════════════════
    // CALENDAR MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/calendar")
    public String calendar(Model model, 
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        int currentMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        int currentYear = (year != null) ? year : LocalDate.now().getYear();
        
        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("departments", departmentRepository.findAll());
        return "hr/calendar";
    }

    @GetMapping("/calendar/events")
    @ResponseBody
    public ResponseEntity<List<HrEventResponseDTO>> getCalendarEvents(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(calendarService.getEventsByMonth(month, year));
    }

    @PostMapping("/calendar/create")
    public String createEvent(
            @ModelAttribute HrEventCreateDTO dto,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {
        try {
            calendarService.createEvent(dto, resolveCurrentUserId(principal));
            ra.addFlashAttribute("successMessage", "Event created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to create event: " + e.getMessage());
        }
        return "redirect:/hr/calendar";
    }

    @PostMapping("/calendar/update")
    public String updateEvent(
            @ModelAttribute HrEventUpdateDTO dto,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {
        try {
            calendarService.updateEvent(dto, resolveCurrentUserId(principal));
            ra.addFlashAttribute("successMessage", "Event updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to update event: " + e.getMessage());
        }
        return "redirect:/hr/calendar";
    }

    @PostMapping("/calendar/delete")
    public String deleteEvent(
            @RequestParam Long id,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {
        try {
            calendarService.deleteEvent(id, resolveCurrentUserId(principal));
            ra.addFlashAttribute("successMessage", "Event deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to delete event: " + e.getMessage());
        }
        return "redirect:/hr/calendar";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REPORTS & ANALYTICS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/reports")
    public String reports(Model model,
            @RequestParam(defaultValue = "overview") String tab,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String reviewPeriod) {

        int reportYear = year != null ? year : LocalDate.now().getYear();
        model.addAttribute("selectedYear", reportYear);
        model.addAttribute("activeTab", tab);
        model.addAttribute("currentYear", LocalDate.now().getYear());

        switch (tab) {
            case "attendance" -> {
                LocalDate from = dateFrom != null ? dateFrom : LocalDate.now().minusDays(29);
                LocalDate to = dateTo != null ? dateTo : LocalDate.now();
                model.addAttribute("dateFrom", from);
                model.addAttribute("dateTo", to);
                model.addAttribute("report", reportService.getAttendanceReport(from, to));
            }
            case "leave" -> model.addAttribute("report", reportService.getLeaveReport(reportYear));
            case "payroll" -> model.addAttribute("report", reportService.getPayrollReport());
            case "performance" -> model.addAttribute("report", reportService.getPerformanceReport(reviewPeriod));
            case "saved" -> model.addAttribute("history", reportService.getAllReports());
            default -> model.addAttribute("report", reportService.getOverviewReport(reportYear));
        }
        return "hr/reports";
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReportPdf(
            @RequestParam(defaultValue = "overview") String tab,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        int reportYear = year != null ? year : LocalDate.now().getYear();

        // Build model data for PDF
        Context ctx = new Context();
        ctx.setVariable("selectedYear", reportYear);
        ctx.setVariable("activeTab", tab);
        ctx.setVariable("exportDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        switch (tab) {
            case "attendance" -> {
                LocalDate from = dateFrom != null ? dateFrom : LocalDate.now().minusDays(29);
                LocalDate to = dateTo != null ? dateTo : LocalDate.now();
                ctx.setVariable("dateFrom", from);
                ctx.setVariable("dateTo", to);
                ctx.setVariable("report", reportService.getAttendanceReport(from, to));
            }
            case "leave" -> ctx.setVariable("report", reportService.getLeaveReport(reportYear));
            case "payroll" -> ctx.setVariable("report", reportService.getPayrollReport());
            case "performance" -> ctx.setVariable("report", reportService.getPerformanceReport(null));
            default -> ctx.setVariable("report", reportService.getOverviewReport(reportYear));
        }

        // Render Thymeleaf template to HTML
        String html = templateEngine.process("hr/reports-pdf", ctx);

        // Convert HTML to PDF via Flying Saucer
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);

            String filename = "HR_Report_" + tab + "_" + reportYear + ".pdf";



            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new ReportExportException("Failed to generate PDF report: " + e.getMessage());
        }
    }

    @PostMapping("/reports/prepare")
    public String prepareReport(
            @RequestParam String tab,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam String title,
            @RequestParam String remarks,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            com.group5.ems.entity.User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Long employeeId = (user.getEmployee() != null) ? user.getEmployee().getId() : null;
            
            reportService.saveReportDraft(tab, year, dateFrom, dateTo, title, remarks, employeeId);
            redirectAttributes.addFlashAttribute("success", "Report draft saved successfully for professional review.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to prepare report: " + e.getMessage());
        }
        
        return "redirect:/hr/reports?tab=saved";
    }

    @PostMapping("/reports/{id}/publish")
    public String publishReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reportService.publishReport(id);
            redirectAttributes.addFlashAttribute("success", "Report published and HR Manager notified.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to publish report: " + e.getMessage());
        }
        return "redirect:/hr/reports?tab=saved";
    }

    @GetMapping("/reports/download/{id}")
    public ResponseEntity<byte[]> downloadSavedReport(@PathVariable Long id) {
        byte[] bytes = reportService.getReportFileBytes(id);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"HR_Report_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

}


