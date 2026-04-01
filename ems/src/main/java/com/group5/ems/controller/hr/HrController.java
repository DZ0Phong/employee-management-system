package com.group5.ems.controller.hr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.group5.ems.dto.response.ApplicationStageDTO;
import com.group5.ems.dto.response.CandidateCvDTO;
import com.group5.ems.dto.response.HrDashboardMetricsDTO;
import com.group5.ems.dto.response.HrEmployeeDTO;
import com.group5.ems.dto.response.HrEmployeeDetailDTO;
import com.group5.ems.dto.response.HrLeaveRequestDTO;
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
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.admin.AdminService;
import com.group5.ems.service.external.VietQrApiClient;
import com.group5.ems.service.hr.HrAttendanceService;
import com.group5.ems.service.hr.HrBankDetailsService;
import com.group5.ems.service.hr.HrDashboardService;
import com.group5.ems.service.hr.HrEmployeeService;
import com.group5.ems.service.hr.HrLeaveService;
import com.group5.ems.service.hr.HrPayrollService;
import com.group5.ems.service.hr.HrPerformanceService;
import com.group5.ems.service.hr.HrRecruitmentService;
import com.group5.ems.service.hr.HrRequestService;

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

        List<Department> departments = departmentRepository.findAll();
        model.addAttribute("departments", departments);

        return "hr/employees";
    }

    @GetMapping("/api/employees/search")
    @ResponseBody
    public ResponseEntity<List<HrEmployeeDTO>> searchEmployeesApi(@RequestParam String q) {
        Pageable pageable = PageRequest.of(0, 5);
        Page<HrEmployeeDTO> page = employeeService.searchEmployees(q, null, null, pageable);
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
                             @RequestParam(required = false) String department,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") int page) {
        
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        
        model.addAttribute("stats", attendanceService.getAttendanceStats(queryDate));
        
        Pageable pageable = PageRequest.of(page, EMPLOYEE_PAGE_SIZE);
        org.springframework.data.domain.Page<com.group5.ems.dto.response.HrAttendanceDetailDTO> attendancePage = attendanceService.getAttendanceRecords(queryDate, search, department, status, pageable);
        
        model.addAttribute("attendances", attendancePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", attendancePage.getTotalPages());
        model.addAttribute("totalItems", attendancePage.getTotalElements());
        
        model.addAttribute("date", queryDate);
        model.addAttribute("search", search);
        model.addAttribute("department", department);
        model.addAttribute("status", status);
        model.addAttribute("departments", departmentRepository.findAll());
        
        return "hr/attendance";
    }

    @GetMapping("/leave")
    public String leave(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String leaveType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        // Pending requests (current tab)
        model.addAttribute("pendingLeaves", leaveService.getPendingLeaves());

        // HRM Pending (New tab - read only)
        model.addAttribute("hrmPendingLeaves", leaveService.getHrmPendingLeaves());

        // Filtered history (server-side — improvement #2)
        Pageable pageable = PageRequest.of(page, EMPLOYEE_PAGE_SIZE);
        Page<HrLeaveRequestDTO> historyPage = leaveService.getLeaveHistoryFiltered(
                status, departmentId, leaveType, search, dateFrom, dateTo, pageable);
        model.addAttribute("leaveHistory", historyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", historyPage.getTotalPages());
        model.addAttribute("totalItems", historyPage.getTotalElements());

        // Statistics (improvement #6)
        model.addAttribute("leaveStats", leaveService.getLeaveStats());

        // Leave balance summary (improvement #1)
        model.addAttribute("balanceSummary", leaveService.getLeaveBalanceSummary());

        // Rejection categories for the modal dropdown (improvement #4)
        model.addAttribute("rejectionCategories", leaveService.getRejectionCategories());

        // Filter state preservation
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterDepartmentId", departmentId);
        model.addAttribute("filterLeaveType", leaveType);
        model.addAttribute("filterSearch", search);
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        model.addAttribute("departments", departmentRepository.findAll());

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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(leaveService.getCalendarEvents(start, end));
    }

    // ── CSV export (improvement #7) ──

    @GetMapping("/leave/export")
    public void exportLeaveCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"leave-history-" + LocalDate.now() + ".csv\"");
        leaveService.exportLeaveHistoryToCsv(status, departmentId, response.getWriter());
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
    public String recruitment(Model model) {
        model.addAttribute("activeJobs", recruitmentService.getActiveJobPosts());
        model.addAttribute("totalOpenJobs", recruitmentService.countOpenJobs());
        model.addAttribute("recentApplicants", recruitmentService.getRecentApplications());
        model.addAttribute("totalApplications", recruitmentService.countTotalApplications());
        model.addAttribute("interviewers", recruitmentService.getAvailableInterviewers());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("positions", positionRepository.findAll());
        model.addAttribute("jobPostRequests", recruitmentService.getJobPostRequests());
        model.addAttribute("pendingJobRequests", recruitmentService.countPendingJobRequests());
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
                                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
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

}
