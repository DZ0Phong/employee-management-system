package com.group5.ems.repository;

import com.group5.ems.entity.HrReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrReportRepository extends JpaRepository<HrReport, Long> {
    List<HrReport> findAllByOrderByGeneratedAtDesc();
    List<HrReport> findByReportTypeOrderByGeneratedAtDesc(String reportType);
    List<HrReport> findByIsPublishedTrueOrderByPublishedAtDesc();
}
