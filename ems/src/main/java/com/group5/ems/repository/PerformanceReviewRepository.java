package com.group5.ems.repository;

import com.group5.ems.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {

    List<PerformanceReview> findByEmployeeId(Long employeeId);

    Optional<PerformanceReview> findByEmployeeIdAndReviewPeriod(Long employeeId, String reviewPeriod);

    List<PerformanceReview> findByReviewerId(Long reviewerId);
    List<PerformanceReview> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    Optional<PerformanceReview> findTopByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
