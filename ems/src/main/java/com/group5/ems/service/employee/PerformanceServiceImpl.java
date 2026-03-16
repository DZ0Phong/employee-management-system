package com.group5.ems.service.employee;

import com.group5.ems.dto.response.PerformanceReviewDTO;
import com.group5.ems.dto.response.PerformanceSummaryDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.PerformanceReview;
import com.group5.ems.repository.EmployeeKpiRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.PerformanceReviewRepository;
import com.group5.ems.service.employee.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeKpiRepository kpiRepository;

    @Override
    @Transactional(readOnly = true)
    public PerformanceSummaryDTO getPerformanceSummary(Long employeeId) {
        List<PerformanceReview> reviews = reviewRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);

        BigDecimal currentRating = BigDecimal.ZERO;
        BigDecimal previousRating = BigDecimal.ZERO;
        String talentMatrix = "N/A";

        if (!reviews.isEmpty()) {
            currentRating = reviews.get(0).getPerformanceScore();
            talentMatrix = reviews.get(0).getTalentMatrix() != null ? reviews.get(0).getTalentMatrix() : "N/A";
        }
        if (reviews.size() >= 2) {
            previousRating = reviews.get(1).getPerformanceScore();
        }

        // KPI stats
        long kpisTotal = kpiRepository.findByEmployeeId(employeeId).size();
        long kpisMet = kpiRepository.findByEmployeeId(employeeId).stream()
                .filter(k -> "COMPLETED".equals(k.getStatus()))
                .count();

        // Skills count
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        int skillsCount = (employee != null && employee.getUser() != null) ? 0 : 0;
        // TODO: inject EmployeeSkillRepository khi cần

        return PerformanceSummaryDTO.builder()
                .currentRating(currentRating)
                .previousRating(previousRating)
                .talentMatrix(talentMatrix)
                .totalReviews(reviews.size())
                .kpisMet((int) kpisMet)
                .kpisTotal((int) kpisTotal)
                .skillsCount(skillsCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerformanceReviewDTO> getReviewHistory(Long employeeId) {
        return reviewRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ── Helper ─────────────────────────────────────────────
    private PerformanceReviewDTO mapToDTO(PerformanceReview r) {
        String reviewerName = "";
        if (r.getReviewer() != null && r.getReviewer().getUser() != null) {
            reviewerName = r.getReviewer().getUser().getFullName();
        }

        return PerformanceReviewDTO.builder()
                .id(r.getId())
                .reviewPeriod(r.getReviewPeriod())
                .reviewerName(reviewerName)
                .performanceScore(r.getPerformanceScore())
                .potentialScore(r.getPotentialScore())
                .talentMatrix(r.getTalentMatrix())
                .strengths(r.getStrengths())
                .areasToImprove(r.getAreasToImprove())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}