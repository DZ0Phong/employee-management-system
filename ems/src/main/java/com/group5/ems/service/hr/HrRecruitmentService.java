package com.group5.ems.service.hr;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
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
import com.group5.ems.dto.response.InterviewerDTO;
import com.group5.ems.entity.Application;
import com.group5.ems.entity.ApplicationStage;
import com.group5.ems.entity.CandidateCv;
import com.group5.ems.entity.InterviewAssignment;
import com.group5.ems.entity.JobPost;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.RequestApprovalHistory;
import com.group5.ems.repository.ApplicationRepository;
import com.group5.ems.repository.ApplicationStageRepository;
import com.group5.ems.repository.CandidateCvRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.InterviewAssignmentRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.RequestApprovalHistoryRepository;
import com.group5.ems.repository.RequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrRecruitmentService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final JobPostRepository jobPostRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationStageRepository applicationStageRepository;
    private final CandidateCvRepository candidateCvRepository;
    private final InterviewAssignmentRepository interviewAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final RequestRepository requestRepository;
    private final RequestApprovalHistoryRepository requestApprovalHistoryRepository;

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

    /**
     * Danh sách employee cho assign modal.
     *
     * Mapping schema thực tế:
     * employees.user_id → users.id
     * users.full_name → InterviewerDTO.fullName
     * users.avatar_url → InterviewerDTO.avatarUrl
     * employees.employee_code → InterviewerDTO.employeeCode
     * positions.name → InterviewerDTO.jobTitle
     * departments.name → InterviewerDTO.departmentName
     *
     * interview_assignments.interviewer_id → users.id (KHÔNG phải employees.id)
     * Employees không có user account bị loại trừ.
     */
    public List<InterviewerDTO> getAvailableInterviewers() {
        return employeeRepository.findAllWithUser().stream()
                .map(emp -> {
                    if (emp.getUser() == null)
                        return null;

                    String name = emp.getUser().getFullName();
                    if (name == null || name.isBlank())
                        return null;

                    return new InterviewerDTO(
                            emp.getUser().getId(), // users.id → interviewer_id FK
                            name, // users.full_name
                            buildInitials(name),
                            emp.getEmployeeCode(), // employees.employee_code
                            emp.getPosition() != null ? emp.getPosition().getName() : null,
                            emp.getDepartment() != null ? emp.getDepartment().getName() : null,
                            emp.getUser().getAvatarUrl() // users.avatar_url
                    );
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Interviewers đang được assign cho một application.
     * employeeCode / jobTitle / departmentName = null vì chỉ join từ User,
     * không join thêm sang Employee để tránh thêm query.
     */
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
                            u.getId(),
                            name,
                            buildInitials(name),
                            null,
                            null,
                            null,
                            u.getAvatarUrl());
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignInterviewers(Long applicationId,
            List<Long> interviewerIds,
            Long assignedBy) {
        applicationRepository.findById(applicationId)
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
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. JOB POST REQUESTS
    // ══════════════════════════════════════════════════════════════════════════

    public List<HrJobRequestDTO> getJobPostRequests() {
        return requestRepository
                .findByRequestType_CodeOrderByCreatedAtDesc("RECRUITMENT")
                .stream()
                .map(this::mapToJobRequestDTO)
                .collect(Collectors.toList());
    }

    public long countPendingJobRequests() {
        return requestRepository
                .countByRequestType_CodeAndStatus("RECRUITMENT", "PENDING");
    }

    private HrJobRequestDTO mapToJobRequestDTO(Request req) {
        String submitter = "";
        String department = "";

        if (req.getEmployee() != null) {
            var emp = req.getEmployee();
            // Ưu tiên full_name từ User, fallback sang employee_code
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
                .submittedAtFormatted(req.getCreatedAt() != null
                        ? req.getCreatedAt().format(DATETIME_FMT)
                        : "")
                .updatedAtFormatted(req.getUpdatedAt() != null
                        ? req.getUpdatedAt().format(DATETIME_FMT)
                        : "")
                .build();
    }

    @Transactional
    public void approveJobRequest(Long requestId, Long approverId) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        req.setStatus("APPROVED");
        req.setApprovedBy(approverId);
        req.setApprovedAt(LocalDateTime.now());
        requestRepository.save(req);

        // Ghi lịch sử
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
                .orElseThrow(() -> new IllegalArgumentException("Job post not found: " + id));
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
                .orElseThrow(() -> new IllegalArgumentException("Job post not found: " + id));
        String title = job.getTitle();
        jobPostRepository.deleteById(id);
        return title;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAPPERS
    // ══════════════════════════════════════════════════════════════════════════

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

    /**
     * Map Application → HrApplicantDTO.
     *
     * Candidate entity thực tế có các field:
     * fullName, email, phone, headline, summary,
     * yearsExperience, expectedSalary,
     * address, dateOfBirth, introduction
     */
    private HrApplicantDTO mapToApplicantDTO(Application app) {
        String name = "Unknown";
        String email = "";
        String phone = "";
        String initials = "?";
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

        // Lấy stage mới nhất từ ApplicationStage; fallback về application.status
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
                .appliedDateFormatted(app.getAppliedAt() != null
                        ? app.getAppliedAt().format(DATE_FMT)
                        : "")
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
            return formatVnd(min) + " – " + formatVnd(max) + " VND";
        if (min != null)
            return "From " + formatVnd(min) + " VND";
        return "Up to " + formatVnd(max) + " VND";
    }

    private String formatVnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount);
    }
}