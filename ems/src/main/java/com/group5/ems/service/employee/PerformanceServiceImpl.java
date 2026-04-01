package com.group5.ems.service.employee;

import com.group5.ems.dto.response.PerformanceReviewDTO;
import com.group5.ems.dto.response.PerformanceSummaryDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.PerformanceReview;
import com.group5.ems.repository.EmployeeKpiRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.EmployeeSkillRepository;
import com.group5.ems.repository.PerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeKpiRepository kpiRepository;
    private final EmployeeSkillRepository employeeSkillRepository;

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

        long kpisTotal = kpiRepository.findByEmployeeId(employeeId).size();
        long kpisMet = kpiRepository.findByEmployeeId(employeeId).stream()
                .filter(kpi -> "COMPLETED".equalsIgnoreCase(kpi.getStatus()))
                .count();

        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        int skillsCount = employee != null ? employeeSkillRepository.findByEmployeeId(employee.getId()).size() : 0;

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

    private PerformanceReviewDTO mapToDTO(PerformanceReview review) {
        String reviewerName = "";
        if (review.getReviewer() != null && review.getReviewer().getUser() != null) {
            reviewerName = review.getReviewer().getUser().getFullName();
        }

        return PerformanceReviewDTO.builder()
                .id(review.getId())
                .reviewPeriod(review.getReviewPeriod())
                .reviewPeriodDisplay(prettifyReviewPeriod(review.getReviewPeriod()))
                .reviewerName(reviewerName)
                .performanceScore(review.getPerformanceScore())
                .potentialScore(review.getPotentialScore())
                .talentMatrix(review.getTalentMatrix())
                .strengths(review.getStrengths())
                .areasToImprove(review.getAreasToImprove())
                .status(review.getStatus())
                .statusDisplay(prettifyStatus(review.getStatus()))
                .createdAt(review.getCreatedAt())
                .build();
    }

    private String prettifyStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Draft";
        }
        return switch (status.trim().toUpperCase(Locale.ENGLISH)) {
            case "COMPLETED" -> "Completed";
            case "DRAFT" -> "Draft";
            case "SCHEDULED" -> "Scheduled";
            default -> status.replace('_', ' ');
        };
    }

    private String prettifyReviewPeriod(String reviewPeriod) {
        if (reviewPeriod == null || reviewPeriod.isBlank()) {
            return "Not set";
        }
        String normalized = reviewPeriod.trim().toUpperCase(Locale.ENGLISH);
        if (normalized.startsWith("YEAR_")) {
            return "Year " + normalized.substring(5);
        }
        if (normalized.startsWith("H1_")) {
            return "H1 " + normalized.substring(3);
        }
        if (normalized.startsWith("H2_")) {
            return "H2 " + normalized.substring(3);
        }
        return reviewPeriod.replace('_', ' ');
    }
}
