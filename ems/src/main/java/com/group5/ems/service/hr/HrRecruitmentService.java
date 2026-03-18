package com.group5.ems.service.hr;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group5.ems.dto.response.HrApplicantDTO;
import com.group5.ems.dto.response.HrRecruitmentDTO;
import com.group5.ems.entity.Application;
import com.group5.ems.entity.ApplicationStage;
import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.ApplicationRepository;
import com.group5.ems.repository.JobPostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrRecruitmentService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final JobPostRepository jobPostRepository;
    private final ApplicationRepository applicationRepository;


    public List<HrRecruitmentDTO> getActiveJobPosts() {
        return jobPostRepository.findByStatus("OPEN").stream()
                .map(this::mapToJobPostDTO)
                .collect(Collectors.toList());
    }

    public long countOpenJobs() {
        return jobPostRepository.countByStatus("OPEN");
    }

    public List<HrApplicantDTO> getRecentApplications() {
        List<Application> apps = applicationRepository.findAllWithDetails(
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "appliedAt"))).getContent();

        return apps.stream()
                .map(this::mapToApplicantDTO)
                .collect(Collectors.toList());
    }

    public long countTotalApplications() {
        return applicationRepository.count();
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private HrRecruitmentDTO mapToJobPostDTO(JobPost job) {
        String departmentName = (job.getDepartment() != null) ? job.getDepartment().getName() : "N/A";

        // Dùng query đếm thay vì load lazy collection — tránh N+1
        int applicantCount = (int) applicationRepository.countByJobPostId(job.getId());

        String salaryRange = formatSalaryRange(job.getSalaryMin(), job.getSalaryMax());

        Long daysUntilClose = null;
        if (job.getCloseDate() != null) {
            daysUntilClose = ChronoUnit.DAYS.between(LocalDate.now(), job.getCloseDate());
        }

        return HrRecruitmentDTO.builder()
                .id(job.getId())
                .jobTitle(job.getTitle())
                .department(departmentName)
                .position(job.getPosition() != null ? job.getPosition().getName() : null)
                .status(job.getStatus())
                .applicantCount(applicantCount)
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryRange(salaryRange)
                .openDate(job.getOpenDate())
                .openDateFormatted(job.getOpenDate() != null ? job.getOpenDate().format(DATE_FMT) : "")
                .closeDate(job.getCloseDate())
                .daysUntilClose(daysUntilClose)
                .closeDateFormatted(job.getCloseDate() != null ? job.getCloseDate().format(DATE_FMT) : "")
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .benefits(job.getBenefits())
                .build();
    }

    private HrApplicantDTO mapToApplicantDTO(Application app) {
        String name = "Unknown";
        String email = "";
        String phone = "";
        String initials = "?";
        Integer yearsExp = null;
        BigDecimal expectedSalary = null;

        if (app.getCandidate() != null) {
            name = app.getCandidate().getFullName();
            email = app.getCandidate().getEmail();
            phone = app.getCandidate().getPhone();
            yearsExp = app.getCandidate().getYearsExperience();
            expectedSalary = app.getCandidate().getExpectedSalary();

            if (name != null && !name.isBlank()) {
                String[] parts = name.trim().split("\\s+");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(2, parts.length); i++) {
                    if (!parts[i].isEmpty())
                        sb.append(parts[i].charAt(0));
                }
                initials = sb.toString().toUpperCase();
            }
        }

        String jobTitle = "Unknown Position";
        String department = "";

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
                .expectedSalary(expectedSalary)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String formatSalaryRange(BigDecimal min, BigDecimal max) {
        if (min == null && max == null)
            return null;
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        if (min != null && max != null) {
            return fmt.format(min) + " – " + fmt.format(max) + " VND";
        } else if (min != null) {
            return "From " + fmt.format(min) + " VND";
        } else {
            return "Up to " + fmt.format(max) + " VND";
        }
    }

    // ── Stage update ──────────────────────────────────────────────────────

    @Transactional
    public void updateApplicationStage(Long applicationId, String newStage, String note) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        app.setStatus(newStage);

        ApplicationStage stageEntry = new ApplicationStage();
        stageEntry.setApplicationId(applicationId);
        stageEntry.setStageName(newStage);
        stageEntry.setNote(note);
        stageEntry.setApplication(app);
        app.getStages().add(stageEntry);

        applicationRepository.save(app);
    }
}