package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.*;
import com.group5.ems.entity.Event;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.EventRepository;
import com.group5.ems.repository.JobPostRepository;
import com.group5.ems.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HRAnalyticsService {

    private final EmployeeRepository employeeRepository;
    private final JobPostRepository jobPostRepository;
    private final SalaryRepository salaryRepository;
    private final EventRepository eventRepository;

    // ── KPI ──────────────────────────────────────────────────────────────────
    public AnalyticsKpiDTO getKpiData() {
        // totalWorkforce lấy từ DB
        long totalWorkforce = employeeRepository.countByStatus("ACTIVE");

        // openPositions lấy từ DB
        int openPositions = jobPostRepository.findByStatus("OPEN").size();
        
        // averageSalary lấy từ DB
        Double avgSalary = salaryRepository.getAverageSalary();
        String avgSalaryFormatted = avgSalary != null 
                ? NumberFormat.getCurrencyInstance(Locale.US).format(avgSalary)
                : "$0";

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

                .averageSalary(avgSalaryFormatted)
                .salaryChange("-1.5%")
                .salaryChangePositive(false)
                .build();
    }

    // ── Biểu đồ phòng ban ────────────────────────────────────────────────────
    public DeptDataDTO getDeptData() {
        List<Object[]> results = employeeRepository.countEmployeeByDepartmentName();
        
        if (results.isEmpty()) {
            // Fallback nếu không có data
            return DeptDataDTO.builder()
                    .labels(Arrays.asList("No Data"))
                    .counts(Arrays.asList(0))
                    .build();
        }
        
        List<String> labels = results.stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());
        
        List<Integer> counts = results.stream()
                .map(row -> ((Number) row[1]).intValue())
                .collect(Collectors.toList());
        
        return DeptDataDTO.builder()
                .labels(labels)
                .counts(counts)
                .build();
    }

    // ── Biểu đồ lương ────────────────────────────────────────────────────────
    public SalaryDataDTO getSalaryData() {
        List<Object[]> results = salaryRepository.countBySalaryBand();
        
        if (results.isEmpty()) {
            // Fallback
            return SalaryDataDTO.builder()
                    .labels(Arrays.asList("<50k", "50-80k", "80-110k", "110-150k", "150k+"))
                    .counts(Arrays.asList(0, 0, 0, 0, 0))
                    .build();
        }
        
        // Ensure all bands are present in correct order
        Map<String, Integer> bandMap = new HashMap<>();
        for (Object[] row : results) {
            bandMap.put((String) row[0], ((Number) row[1]).intValue());
        }
        
        List<String> labels = Arrays.asList("<50k", "50-80k", "80-110k", "110-150k", "150k+");
        List<Integer> counts = labels.stream()
                .map(label -> bandMap.getOrDefault(label, 0))
                .collect(Collectors.toList());
        
        return SalaryDataDTO.builder()
                .labels(labels)
                .counts(counts)
                .build();
    }

    // ── Biểu đồ diversity ────────────────────────────────────────────────────
    public DiversityDataDTO getDiversityData() {
        // Employee entity không có gender field, dùng static data
        return DiversityDataDTO.builder()
                .labels(Arrays.asList("Male", "Female", "Other"))
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
        List<Event> policyEvents = eventRepository.findPolicyReviews();
        
        if (policyEvents.isEmpty()) {
            return Collections.emptyList();
        }
        
        return policyEvents.stream()
                .map(event -> PolicyReviewDTO.builder()
                        .id(event.getId())
                        .name(event.getTitle())
                        .owner(event.getDescription() != null ? event.getDescription() : "HR Team")
                        .status(event.getStatus() != null ? event.getStatus() : "DRAFTING")
                        .deadline(event.getStartDate())
                        .build())
                .collect(Collectors.toList());
    }
}