package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.*;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.JobPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HRAnalyticsService {

    private final EmployeeRepository employeeRepository;
    private final JobPostRepository jobPostRepository;

    // ── KPI ──────────────────────────────────────────────────────────────────
    public AnalyticsKpiDTO getKpiData() {
        // totalWorkforce lấy từ DB
        long totalWorkforce = employeeRepository.countByStatus("ACTIVE");

        // openPositions lấy từ DB
        int openPositions = jobPostRepository.findByStatus("OPEN").size();

        return AnalyticsKpiDTO.builder()
                .totalWorkforce((int) totalWorkforce)
                .workforceChange("+2.4%")
                .workforceChangePositive(true)

                .retentionRate("94.2%")
                .retentionChange("+0.8%")
                .retentionChangePositive(true)

                .openPositions(openPositions)
                .hiringVelocity("12%")
                .hiringVelocityPositive(false)

                .averageSalary("$85,500")
                .salaryChange("-1.5%")
                .salaryChangePositive(false)
                .build();
    }

    // ── Biểu đồ phòng ban ────────────────────────────────────────────────────
    public DeptDataDTO getDeptData() {
        // TODO: thay bằng query từ DB sau
        return DeptDataDTO.builder()
                .labels(Arrays.asList("Engineering", "Sales", "Marketing", "Operations", "Finance"))
                .counts(Arrays.asList(578, 321, 192, 128, 65))
                .build();
    }

    // ── Biểu đồ lương ────────────────────────────────────────────────────────
    public SalaryDataDTO getSalaryData() {
        // TODO: thay bằng query từ DB sau
        return SalaryDataDTO.builder()
                .labels(Arrays.asList("<50k", "50-80k", "80-110k", "110-150k", "150k+"))
                .counts(Arrays.asList(120, 410, 490, 190, 74))
                .build();
    }

    // ── Biểu đồ diversity ────────────────────────────────────────────────────
    public DiversityDataDTO getDiversityData() {
        // TODO: thay bằng query từ DB sau
        return DiversityDataDTO.builder()
                .labels(Arrays.asList("Male", "Female", "Non-binary"))
                .values(Arrays.asList(54, 42, 4))
                .colors(Arrays.asList("#1414b8", "rgba(20,20,184,0.55)", "#cbd5e1"))
                .build();
    }

    // ── Training courses ─────────────────────────────────────────────────────
    public List<TrainingCourseDTO> getTrainingCourses() {
        // TODO: thay bằng query từ DB sau
        return Arrays.asList(
                TrainingCourseDTO.builder().name("Cybersecurity Awareness").completionRate(88).build(),
                TrainingCourseDTO.builder().name("Diversity & Inclusion 101").completionRate(92).build(),
                TrainingCourseDTO.builder().name("Leadership Essentials").completionRate(64).build()
        );
    }

    // ── Policy reviews ───────────────────────────────────────────────────────
    public List<PolicyReviewDTO> getPolicyReviews() {
        // TODO: thay bằng query từ DB sau
        return Arrays.asList(
                PolicyReviewDTO.builder()
                        .id(1L).name("Remote Work Policy v2.4")
                        .owner("HR Legal Team").status("IN_REVIEW")
                        .deadline(java.time.LocalDate.of(2024, 10, 12))
                        .build(),
                PolicyReviewDTO.builder()
                        .id(2L).name("Annual Bonus Structure")
                        .owner("Finance & HR").status("DRAFTING")
                        .deadline(java.time.LocalDate.of(2024, 11, 1))
                        .build(),
                PolicyReviewDTO.builder()
                        .id(3L).name("Health Benefits 2025")
                        .owner("Benefits Admin").status("FINALIZED")
                        .deadline(null)
                        .build()
        );
    }
}