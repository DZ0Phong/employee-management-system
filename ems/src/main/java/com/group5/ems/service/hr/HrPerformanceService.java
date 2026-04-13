package com.group5.ems.service.hr;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group5.ems.dto.response.HrPerformanceDTO;
import com.group5.ems.entity.PerformanceReview;
import com.group5.ems.repository.PerformanceReviewRepository;
import com.group5.ems.service.common.LogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class HrPerformanceService {

    private final PerformanceReviewRepository reviewRepository;
    private final LogService logService;

    /**
     * Paginated list of reviews with optional search and status filter.
     */
    public Page<HrPerformanceDTO> getReviews(String search, String status, Long departmentId, Long reviewerId,
                                            String reviewPeriod, BigDecimal minScore, BigDecimal maxScore,
                                            BigDecimal minPotential, BigDecimal maxPotential, Pageable pageable) {
        String s = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String st = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String rp = (reviewPeriod != null && !reviewPeriod.trim().isEmpty()) ? reviewPeriod.trim() : null;

        Page<PerformanceReview> page = reviewRepository.searchAdvanced(s, st, departmentId, reviewerId, rp, minScore, maxScore, minPotential, maxPotential, pageable);

        List<HrPerformanceDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    /**
     * Single review by ID.
     */
    public Optional<HrPerformanceDTO> getReviewById(Long id) {
        Optional<HrPerformanceDTO> dto = reviewRepository.findById(id).map(this::mapToDTO);
        return dto;
    }




    public long countByStatus(String status) {
        return reviewRepository.countByStatus(status);
    }

    public List<String> getDistinctReviewPeriods() {
        return reviewRepository.findDistinctReviewPeriods();
    }


    // ── Private helper ──────────────────────────────────────────

    private HrPerformanceDTO mapToDTO(PerformanceReview r) {
        String employeeName = "Unknown";
        String employeeCode = "N/A";
        String departmentName = "N/A";
        String reviewerName = "N/A";

        if (r.getEmployee() != null) {
            if (r.getEmployee().getUser() != null && r.getEmployee().getUser().getFullName() != null) {
                employeeName = r.getEmployee().getUser().getFullName();
            }
            if (r.getEmployee().getDepartment() != null) {
                departmentName = r.getEmployee().getDepartment().getName();
            }
            employeeCode = r.getEmployee().getEmployeeCode();
        }
        if (r.getReviewer() != null && r.getReviewer().getUser() != null
                && r.getReviewer().getUser().getFullName() != null) {
            reviewerName = r.getReviewer().getUser().getFullName();
        }

        // Map numeric performance score to letter grade for display
        String letterScore = mapToLetterGrade(r.getPerformanceScore());

        return HrPerformanceDTO.builder()
                .id(r.getId())
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .department(departmentName)
                .reviewerName(reviewerName)
                .status(r.getStatus())
                .score(letterScore)
                .reviewPeriod(r.getReviewPeriod())
                .performanceScore(r.getPerformanceScore())
                .potentialScore(r.getPotentialScore())
                .talentMatrix(r.getTalentMatrix())
                .strengths(r.getStrengths())
                .areasToImprove(r.getAreasToImprove())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private String mapToLetterGrade(BigDecimal score) {
        if (score == null) return "-";
        double val = score.doubleValue();
        if (val >= 4.5) return "A";
        if (val >= 3.5) return "B";
        if (val >= 2.5) return "C";
        if (val >= 1.5) return "D";
        return "F";
    }
}
