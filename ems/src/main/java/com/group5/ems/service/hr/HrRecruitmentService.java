package com.group5.ems.service.hr;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.group5.ems.dto.response.ApplicationStageDTO;
import com.group5.ems.dto.response.CandidateCvDTO;
import com.group5.ems.dto.response.HrApplicantDTO;
import com.group5.ems.dto.response.HrJobRequestDTO;
import com.group5.ems.dto.response.HrRecruitmentDTO;
import com.group5.ems.dto.response.InterviewDTO;
import com.group5.ems.dto.response.InterviewerDTO;
import com.group5.ems.entity.Application;
import com.group5.ems.entity.ApplicationStage;
import com.group5.ems.entity.CandidateCv;
import com.group5.ems.entity.Interview;
import com.group5.ems.entity.InterviewAssignment;
import com.group5.ems.entity.JobPost;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.RequestApprovalHistory;
import com.group5.ems.entity.User;
import com.group5.ems.exception.JobPostException;
import com.group5.ems.repository.ApplicationRepository;
import com.group5.ems.repository.ApplicationStageRepository;
import com.group5.ems.repository.CandidateCvRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.InterviewAssignmentRepository;
import com.group5.ems.repository.InterviewRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.RequestApprovalHistoryRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrRecruitmentService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ── Template codes (phải khớp với cột `code` trong bảng email_templates) ──
    private static final String TPL_INTERVIEW_ASSIGNED = "INTERVIEW_ASSIGNED";
    private static final String TPL_APPLICATION_HIRED = "APPLICATION_HIRED";
    private static final String TPL_APPLICATION_REJECT = "APPLICATION_REJECTED";
    private static final String TPL_NEW_EMPLOYEE_REQ = "NEW_EMPLOYEE_REQUEST";

    private final JobPostRepository jobPostRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationStageRepository applicationStageRepository;
    private final CandidateCvRepository candidateCvRepository;
    private final InterviewAssignmentRepository interviewAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final RequestRepository requestRepository;
    private final RequestApprovalHistoryRepository requestApprovalHistoryRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final HrEmailService emailService;

    // ══════════════════════════════════════════════════════════════════════════
    // 1. JOB POSTS
    // ══════════════════════════════════════════════════════════════════════════

    public List<HrRecruitmentDTO> getActiveJobPosts() {
        return jobPostRepository.findByStatus("OPEN").stream()
                .map(this::mapToJobPostDTO)
                .collect(Collectors.toList());
    }

    public long countOpenJobs() {
        return jobPostRepository.countByStatus("OPEN");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. APPLICATIONS
    // ══════════════════════════════════════════════════════════════════════════

    public List<HrApplicantDTO> getRecentApplications() {
        return applicationRepository
                .findAllWithDetails(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "appliedAt")))
                .getContent().stream()
                .map(this::mapToApplicantDTO)
                .collect(Collectors.toList());
    }

    public long countTotalApplications() {
        return applicationRepository.count();
    }

    /**
     * Cập nhật stage của application.
     * Khi stage = HIRED → gửi email chúc mừng cho candidate
     * → gửi email yêu cầu tạo nhân viên cho tất cả Admin
     * Khi stage = REJECTED → gửi email thông báo từ chối cho candidate
     */
    @Transactional
    public void updateApplicationStage(Long applicationId, String newStage, String note) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        app.setStatus(newStage);

        ApplicationStage entry = new ApplicationStage();
        entry.setApplicationId(applicationId);
        entry.setStageName(newStage);
        entry.setNote(note);
        entry.setApplication(app);
        app.getStages().add(entry);

        applicationRepository.save(app);

        // ── Gửi email thông báo kết quả ──────────────────────────────────────
        if ("HIRED".equalsIgnoreCase(newStage)) {
            sendHiredNotification(app);
            sendNewEmployeeRequestToAdmins(app);
        } else if ("REJECTED".equalsIgnoreCase(newStage)) {
            sendRejectedNotification(app);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. STAGE HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    public List<ApplicationStageDTO> getStageHistory(Long applicationId) {
        return applicationStageRepository
                .findByApplicationIdOrderByChangedAtDesc(applicationId).stream()
                .map(s -> new ApplicationStageDTO(
                        s.getId(),
                        s.getStageName(),
                        s.getNote(),
                        s.getChangedAt() != null ? s.getChangedAt().format(DATETIME_FMT) : "",
                        null))
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. CV MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    public List<CandidateCvDTO> getCvMetadata(Long candidateId) {
        return candidateCvRepository
                .findMetadataByCandidateId(candidateId).stream()
                .map(row -> {
                    String uploadedFmt = "";
                    if (row[3] instanceof LocalDateTime ldt) {
                        uploadedFmt = ldt.format(DATETIME_FMT);
                    } else if (row[3] instanceof java.sql.Timestamp ts) {
                        uploadedFmt = ts.toLocalDateTime().format(DATETIME_FMT);
                    }
                    return new CandidateCvDTO(
                            (Long) row[0],
                            (String) row[1],
                            (String) row[2],
                            uploadedFmt);
                })
                .collect(Collectors.toList());
    }

    public CandidateCv getCvBlob(Long cvId) {
        return candidateCvRepository.findById(cvId)
                .orElseThrow(() -> new IllegalArgumentException("CV not found: " + cvId));
    }

    @Transactional
    public CandidateCv uploadCv(Long candidateId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        try {
            CandidateCv cv = new CandidateCv();
            cv.setCandidateId(candidateId);
            cv.setFileName(file.getOriginalFilename());
            cv.setFileType(file.getContentType());
            cv.setFileData(file.getBytes());
            return candidateCvRepository.save(cv);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save CV: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteCv(Long cvId) {
        if (!candidateCvRepository.existsById(cvId)) {
            throw new IllegalArgumentException("CV not found: " + cvId);
        }
        candidateCvRepository.deleteById(cvId);
    }

    public MediaType resolveMediaType(String fileType) {
        if (fileType == null)
            return MediaType.APPLICATION_OCTET_STREAM;
        return switch (fileType.toLowerCase()) {
            case "application/pdf" -> MediaType.APPLICATION_PDF;
            case "image/png" -> MediaType.IMAGE_PNG;
            case "image/jpeg", "image/jpg" -> MediaType.IMAGE_JPEG;
            case "image/gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. INTERVIEWER ASSIGNMENT
    // ══════════════════════════════════════════════════════════════════════════

    public List<InterviewerDTO> getAvailableInterviewers() {
        return employeeRepository.findAllWithUser().stream()
                .map(emp -> {
                    if (emp.getUser() == null)
                        return null;
                    String name = emp.getUser().getFullName();
                    if (name == null || name.isBlank())
                        return null;
                    return new InterviewerDTO(
                            emp.getUser().getId(),
                            name,
                            buildInitials(name),
                            emp.getEmployeeCode(),
                            emp.getPosition() != null ? emp.getPosition().getName() : null,
                            emp.getDepartment() != null ? emp.getDepartment().getName() : null,
                            emp.getUser().getAvatarUrl());
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<InterviewerDTO> getAssignedInterviewers(Long applicationId) {
        return interviewAssignmentRepository
                .findByApplicationId(applicationId).stream()
                .map(ia -> {
                    var u = ia.getInterviewer();
                    if (u == null)
                        return null;
                    String name = (u.getFullName() != null && !u.getFullName().isBlank())
                            ? u.getFullName()
                            : u.getUsername();
                    return new InterviewerDTO(
                            u.getId(), name, buildInitials(name),
                            null, null, null, u.getAvatarUrl());
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Assign interviewer cho application.
     * Sau khi lưu thành công → gửi email thông báo cho từng interviewer được
     * assign.
     */
    @Transactional
    public void assignInterviewers(Long applicationId, List<Long> interviewerIds, Long assignedBy) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        interviewAssignmentRepository.deleteByApplicationId(applicationId);

        if (interviewerIds == null || interviewerIds.isEmpty())
            return;

        List<InterviewAssignment> toSave = interviewerIds.stream()
                .distinct()
                .map(ivId -> {
                    InterviewAssignment ia = new InterviewAssignment();
                    ia.setApplicationId(applicationId);
                    ia.setInterviewerId(ivId);
                    ia.setAssignedBy(assignedBy);
                    return ia;
                })
                .collect(Collectors.toList());

        interviewAssignmentRepository.saveAll(toSave);

        // ── Gửi email thông báo cho từng interviewer được assign ──────────────
        sendInterviewAssignedEmails(app, interviewerIds);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. JOB POST REQUESTS
    // ══════════════════════════════════════════════════════════════════════════

    public List<HrJobRequestDTO> getJobPostRequests() {
        return requestRepository
                .findByRequestType_CodeInOrderByCreatedAtDesc(List.of("RECRUITMENT", "HR_RECRUIT"))
                .stream()
                .map(this::mapToJobRequestDTO)
                .collect(Collectors.toList());
    }

    public long countPendingJobRequests() {
        return requestRepository
                .countByRequestType_CodeInAndStatus(List.of("RECRUITMENT", "HR_RECRUIT"), "PENDING");
    }

    private HrJobRequestDTO mapToJobRequestDTO(Request req) {
        String submitter = "";
        String department = "";
        if (req.getEmployee() != null) {
            var emp = req.getEmployee();
            if (emp.getUser() != null && emp.getUser().getFullName() != null) {
                submitter = emp.getUser().getFullName();
            } else {
                submitter = emp.getEmployeeCode();
            }
            if (emp.getDepartment() != null) {
                department = emp.getDepartment().getName();
            }
        }
        return HrJobRequestDTO.builder()
                .id(req.getId())
                .title(req.getTitle())
                .content(req.getContent())
                .status(req.getStatus())
                .step(req.getStep())
                .urgent(req.isUrgent())
                .employeeId(req.getEmployeeId())
                .requestedByName(submitter)
                .departmentName(department)
                .rejectedReason(req.getRejectedReason())
                .submittedAtFormatted(req.getCreatedAt() != null ? req.getCreatedAt().format(DATETIME_FMT) : "")
                .updatedAtFormatted(req.getUpdatedAt() != null ? req.getUpdatedAt().format(DATETIME_FMT) : "")
                .build();
    }

    @Transactional
    public void approveJobRequest(Long requestId, Long approverId) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
        req.setStatus("APPROVED");
        req.setStep("DONE");
        req.setApprovedBy(approverId);
        req.setApprovedAt(LocalDateTime.now());
        requestRepository.save(req);

        RequestApprovalHistory hist = new RequestApprovalHistory();
        hist.setRequestId(requestId);
        hist.setApproverId(approverId);
        hist.setAction("APPROVED");
        hist.setComment("Approved by HR");
        requestApprovalHistoryRepository.save(hist);
    }

    @Transactional
    public void rejectJobRequest(Long requestId, String reason, Long approverId) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
        req.setStatus("REJECTED");
        req.setStep("DONE");
        req.setRejectedReason(reason);
        requestRepository.save(req);

        RequestApprovalHistory hist = new RequestApprovalHistory();
        hist.setRequestId(requestId);
        hist.setApproverId(approverId != null ? approverId : 0L);
        hist.setAction("REJECTED");
        hist.setComment(reason);
        requestApprovalHistoryRepository.save(hist);
    }

    public List<HrRecruitmentDTO> getAllJobPosts() {
        return jobPostRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::mapToJobPostDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateJobPost(Long id, String title, Long departmentId,
            String description, String requirements, String benefits,
            BigDecimal salaryMin, BigDecimal salaryMax,
            LocalDate openDate, LocalDate closeDate, String status) {
        JobPost job = jobPostRepository.findById(id)
                .orElseThrow(() -> JobPostException.notFound(id));
        job.setTitle(title);
        job.setDepartmentId(departmentId);
        job.setDescription(description);
        job.setRequirements(requirements);
        job.setBenefits(benefits);
        job.setSalaryMin(salaryMin);
        job.setSalaryMax(salaryMax);
        if (openDate != null)
            job.setOpenDate(openDate);
        if (closeDate != null)
            job.setCloseDate(closeDate);
        if (status != null && !status.isBlank())
            job.setStatus(status);
        jobPostRepository.save(job);
    }

    @Transactional
    public String deleteJobPost(Long id) {
        JobPost job = jobPostRepository.findById(id)
                .orElseThrow(() -> JobPostException.notFound(id));
        String title = job.getTitle();
        jobPostRepository.deleteById(id);
        return title;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. INTERVIEW SCHEDULING
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void scheduleInterview(Long applicationId,
            Long interviewerId,
            LocalDateTime scheduledAt,
            String location) {
        applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        Interview iv = interviewRepository
                .findByApplicationIdAndInterviewerId(applicationId, interviewerId)
                .orElse(new Interview());

        iv.setApplicationId(applicationId);
        iv.setInterviewerId(interviewerId);
        iv.setScheduledAt(scheduledAt);
        iv.setLocation(location);
        iv.setStatus("SCHEDULED");
        interviewRepository.save(iv);
    }

    public List<InterviewDTO> getMyInterviews(Long interviewerUserId) {
        List<InterviewAssignment> assignments = interviewAssignmentRepository.findByInterviewerId(interviewerUserId);

        return assignments.stream()
                .map(ia -> interviewRepository
                        .findByApplicationIdOrderByScheduledAtDesc(ia.getApplicationId())
                        .stream()
                        .findFirst()
                        .map(this::mapToInterviewDTO)
                        .orElseGet(() -> mapAssignmentToInterviewDTO(ia)))
                .sorted((a, b) -> {
                    boolean aHas = a.getScheduledAtRaw() != null && !a.getScheduledAtRaw().isBlank();
                    boolean bHas = b.getScheduledAtRaw() != null && !b.getScheduledAtRaw().isBlank();
                    if (aHas && bHas)
                        return b.getScheduledAtRaw().compareTo(a.getScheduledAtRaw());
                    if (aHas)
                        return 1;
                    if (bHas)
                        return -1;
                    return 0;
                })
                .collect(Collectors.toList());
    }

    private InterviewDTO mapAssignmentToInterviewDTO(InterviewAssignment ia) {
        String candidateName = "Unknown", candidateEmail = "", initials = "?";
        String jobTitle = "—", department = "";

        var appOpt = applicationRepository.findById(ia.getApplicationId());
        if (appOpt.isPresent()) {
            var app = appOpt.get();
            if (app.getCandidate() != null) {
                candidateName = app.getCandidate().getFullName() != null
                        ? app.getCandidate().getFullName()
                        : "Unknown";
                candidateEmail = app.getCandidate().getEmail() != null
                        ? app.getCandidate().getEmail()
                        : "";
                initials = buildInitials(candidateName);
            }
            if (app.getJobPost() != null) {
                jobTitle = app.getJobPost().getTitle() != null ? app.getJobPost().getTitle() : "—";
                department = app.getJobPost().getDepartment() != null
                        ? app.getJobPost().getDepartment().getName()
                        : "";
            }
        }

        return new InterviewDTO(
                null, ia.getApplicationId(),
                candidateName, initials, candidateEmail,
                jobTitle, department,
                "", "", "",
                "NOT_SCHEDULED",
                null, null);
    }

    public List<InterviewDTO> getInterviewsByApplication(Long applicationId) {
        return interviewRepository.findByApplicationIdOrderByScheduledAtDesc(applicationId)
                .stream()
                .map(this::mapToInterviewDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void submitFeedback(Long interviewId, String feedback, String status) {
        Interview iv = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        iv.setFeedback(feedback);
        iv.setStatus(status);
        interviewRepository.save(iv);
    }

    @Transactional
    public void cancelInterview(Long interviewId) {
        Interview iv = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        iv.setStatus("CANCELLED");
        interviewRepository.save(iv);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. BUSINESS RULE VALIDATION
    // ══════════════════════════════════════════════════════════════════════════

    public void validateCreateJobPost(LocalDate openDate, LocalDate closeDate, String action) {
        if ("publish".equals(action) && openDate != null && closeDate != null) {
            if (closeDate.isBefore(openDate)) {
                throw JobPostException.closeDateBeforeOpenDate();
            }
        }
    }

    public void validateUpdateJobPost(Long jobId, LocalDate openDate, LocalDate closeDate, String newStatus) {
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> JobPostException.notFound(jobId));

        LocalDate effectiveOpen = openDate != null ? openDate : job.getOpenDate();
        LocalDate effectiveClose = closeDate != null ? closeDate : job.getCloseDate();

        if (effectiveOpen != null && effectiveClose != null && effectiveClose.isBefore(effectiveOpen)) {
            throw JobPostException.closeDateBeforeOpenDate();
        }
        if ("OPEN".equals(newStatus) && effectiveClose != null && effectiveClose.isBefore(LocalDate.now())) {
            throw JobPostException.cannotOpenWithPastCloseDate(effectiveClose);
        }
    }

    public void validateDeleteJobPost(Long jobId) {
        long count = applicationRepository.countByJobPostId(jobId);
        if (count > 0) {
            throw JobPostException.hasActiveApplicants(count);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EMAIL HELPERS (private)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Gửi email INTERVIEW_ASSIGNED đến email của từng interviewer được assign.
     * Variables: {{interviewerName}}, {{candidateName}}, {{jobTitle}},
     * {{applicationId}}
     */
    private void sendInterviewAssignedEmails(Application app, List<Long> interviewerIds) {
        String candidateName = resolveCandidateName(app);
        String jobTitle = resolveJobTitle(app);

        for (Long userId : interviewerIds) {
            userRepository.findById(userId).ifPresent(user -> {
                if (user.getEmail() == null || user.getEmail().isBlank())
                    return;

                String interviewerName = user.getFullName() != null ? user.getFullName() : user.getUsername();
                Map<String, String> vars = Map.of(
                        "interviewerName", interviewerName,
                        "candidateName", candidateName,
                        "jobTitle", jobTitle,
                        "applicationId", String.valueOf(app.getId()));
                emailService.sendFromTemplate(user.getEmail(), TPL_INTERVIEW_ASSIGNED, vars);
            });
        }
    }

    /**
     * Gửi email APPLICATION_HIRED đến email của candidate.
     * Variables: {{candidateName}}, {{jobTitle}}, {{companyName}}
     */
    private void sendHiredNotification(Application app) {
        if (app.getCandidate() == null)
            return;
        String email = app.getCandidate().getEmail();
        if (email == null || email.isBlank())
            return;

        Map<String, String> vars = Map.of(
                "candidateName", resolveCandidateName(app),
                "jobTitle", resolveJobTitle(app),
                "companyName", "ERM Pro");
        emailService.sendFromTemplate(email, TPL_APPLICATION_HIRED, vars);
    }

    /**
     * Gửi email APPLICATION_REJECTED đến email của candidate.
     * Variables: {{candidateName}}, {{jobTitle}}, {{companyName}}
     */
    private void sendRejectedNotification(Application app) {
        if (app.getCandidate() == null)
            return;
        String email = app.getCandidate().getEmail();
        if (email == null || email.isBlank())
            return;

        Map<String, String> vars = Map.of(
                "candidateName", resolveCandidateName(app),
                "jobTitle", resolveJobTitle(app),
                "companyName", "ERM Pro");
        emailService.sendFromTemplate(email, TPL_APPLICATION_REJECT, vars);
    }

    /**
     * Gửi email NEW_EMPLOYEE_REQUEST đến tất cả user có role ADMIN.
     * Variables: {{adminName}}, {{candidateName}}, {{jobTitle}}, {{applicationId}}
     */
    private void sendNewEmployeeRequestToAdmins(Application app) {
    String candidateName  = resolveCandidateName(app);
    String jobTitle       = resolveJobTitle(app);
    String candidateEmail = "";
    String candidatePhone = "";
    String role           = "";
    String department     = "";

    if (app.getCandidate() != null) {
        var c = app.getCandidate();
        candidateEmail = c.getEmail()  != null ? c.getEmail()  : "";
        candidatePhone = c.getPhone()  != null ? c.getPhone()  : "";
    }

    if (app.getJobPost() != null) {
        var jp = app.getJobPost();
        if (jp.getPosition() != null)
            role = jp.getPosition().getName();
        if (jp.getDepartment() != null)
            department = jp.getDepartment().getName();
    }

    // ── Gửi cho từng Admin ────────────────────────────────────────────
    List<User> admins = userRepository.findByRoleCode("ADMIN");
    for (User admin : admins) {
        if (admin.getEmail() == null || admin.getEmail().isBlank())
            continue;

        String adminName = admin.getFullName() != null
                ? admin.getFullName()
                : admin.getUsername();

        Map<String, String> vars = Map.of(
                "adminName",      adminName,
                "candidateName",  candidateName,
                "candidateEmail", candidateEmail,
                "candidatePhone", candidatePhone,
                "jobTitle",       jobTitle,
                "role",           role,
                "department",     department,
                "applicationId",  String.valueOf(app.getId()));

        emailService.sendFromTemplate(admin.getEmail(), TPL_NEW_EMPLOYEE_REQ, vars);
    }
}

    // ── small resolvers ───────────────────────────────────────────────────────

    private String resolveCandidateName(Application app) {
        if (app.getCandidate() != null && app.getCandidate().getFullName() != null)
            return app.getCandidate().getFullName();
        return "Candidate";
    }

    private String resolveJobTitle(Application app) {
        if (app.getJobPost() != null && app.getJobPost().getTitle() != null)
            return app.getJobPost().getTitle();
        return "the position";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAPPERS
    // ══════════════════════════════════════════════════════════════════════════

    private InterviewDTO mapToInterviewDTO(Interview iv) {
        String candidateName = "Unknown", candidateEmail = "", initials = "?";
        String jobTitle = "—", department = "";

        if (iv.getApplication() != null) {
            var app = iv.getApplication();
            if (app.getCandidate() != null) {
                candidateName = app.getCandidate().getFullName() != null
                        ? app.getCandidate().getFullName()
                        : "Unknown";
                candidateEmail = app.getCandidate().getEmail() != null
                        ? app.getCandidate().getEmail()
                        : "";
                initials = buildInitials(candidateName);
            }
            if (app.getJobPost() != null) {
                jobTitle = app.getJobPost().getTitle() != null ? app.getJobPost().getTitle() : "—";
                department = app.getJobPost().getDepartment() != null
                        ? app.getJobPost().getDepartment().getName()
                        : "";
            }
        }

        String scheduledFmt = iv.getScheduledAt() != null ? iv.getScheduledAt().format(DATETIME_FMT) : "";
        String scheduledRaw = iv.getScheduledAt() != null
                ? iv.getScheduledAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                : "";

        return new InterviewDTO(
                iv.getId(),
                iv.getApplicationId(),
                candidateName, initials, candidateEmail,
                jobTitle, department,
                scheduledFmt, scheduledRaw,
                iv.getLocation(),
                iv.getStatus(),
                iv.getFeedback(),
                null);
    }

    private HrRecruitmentDTO mapToJobPostDTO(JobPost job) {
        String dept = job.getDepartment() != null ? job.getDepartment().getName() : "N/A";
        int applicants = (int) applicationRepository.countByJobPostId(job.getId());
        String salaryRange = formatSalaryRange(job.getSalaryMin(), job.getSalaryMax());
        Long daysLeft = job.getCloseDate() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), job.getCloseDate())
                : null;
        return HrRecruitmentDTO.builder()
                .id(job.getId())
                .jobTitle(job.getTitle())
                .department(dept)
                .departmentId(job.getDepartmentId())
                .position(job.getPosition() != null ? job.getPosition().getName() : null)
                .status(job.getStatus())
                .applicantCount(applicants)
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryRange(salaryRange)
                .openDate(job.getOpenDate())
                .openDateFormatted(job.getOpenDate() != null ? job.getOpenDate().format(DATE_FMT) : "")
                .closeDate(job.getCloseDate())
                .daysUntilClose(daysLeft)
                .closeDateFormatted(job.getCloseDate() != null ? job.getCloseDate().format(DATE_FMT) : "")
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .benefits(job.getBenefits())
                .build();
    }

    private HrApplicantDTO mapToApplicantDTO(Application app) {
        String name = "Unknown", email = "", phone = "", initials = "?";
        Integer yearsExp = null;
        BigDecimal salary = null;

        if (app.getCandidate() != null) {
            var c = app.getCandidate();
            name = c.getFullName() != null ? c.getFullName() : "Unknown";
            email = c.getEmail() != null ? c.getEmail() : "";
            phone = c.getPhone() != null ? c.getPhone() : "";
            yearsExp = c.getYearsExperience();
            salary = c.getExpectedSalary();
            initials = buildInitials(name);
        }

        String jobTitle = "Unknown Position", department = "";
        if (app.getJobPost() != null) {
            jobTitle = app.getJobPost().getTitle();
            if (app.getJobPost().getDepartment() != null) {
                department = app.getJobPost().getDepartment().getName();
            }
        }

        String stage = app.getStatus();
        if (app.getStages() != null && !app.getStages().isEmpty()) {
            stage = app.getStages().stream()
                    .max((a, b) -> a.getChangedAt().compareTo(b.getChangedAt()))
                    .map(ApplicationStage::getStageName)
                    .orElse(app.getStatus());
        }

        return HrApplicantDTO.builder()
                .id(app.getCandidateId())
                .applicationId(app.getId())
                .applicantName(name)
                .initials(initials)
                .email(email)
                .phone(phone)
                .appliedJob(jobTitle)
                .department(department)
                .appliedDate(app.getAppliedAt())
                .appliedDateFormatted(app.getAppliedAt() != null ? app.getAppliedAt().format(DATE_FMT) : "")
                .stage(stage)
                .yearsExperience(yearsExp)
                .expectedSalary(salary)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private String buildInitials(String name) {
        if (name == null || name.isBlank())
            return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty())
                sb.append(parts[i].charAt(0));
        }
        return sb.toString().toUpperCase();
    }

    private String formatSalaryRange(BigDecimal min, BigDecimal max) {
        if (min == null && max == null)
            return null;
        if (min != null && max != null)
            return formatUsd(min) + " – " + formatUsd(max);
        if (min != null)
            return "From " + formatUsd(min);
        return "Up to " + formatUsd(max);
    }

    private String formatUsd(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }
}