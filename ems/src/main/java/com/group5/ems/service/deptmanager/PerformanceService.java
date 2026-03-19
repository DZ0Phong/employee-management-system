package com.group5.ems.service.deptmanager;

import com.group5.ems.entity.Department;
import com.group5.ems.entity.PerformanceReview;
import com.group5.ems.repository.PerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final DeptManagerUtilService utilService;
    private final PerformanceReviewRepository reviewRepository;

    public Map<String, Object> getPerformanceReviewData() {
        Map<String, Object> data = new HashMap<>();

        // 1. Get current manager and department context
        var currentUser = utilService.getCurrentUser();
        var department = utilService.getDepartmentForManager(currentUser);

        data.put("manager", utilService.getManagerMap(currentUser));
        data.put("pendingApprovals", utilService.getPendingApprovalsCount(department));

        // 2. Fetch all reviews for this department
        List<PerformanceReview> departmentReviews = reviewRepository.findByEmployee_DepartmentIdOrderByUpdatedAtDesc(department.getId());

        // 3. Process Reviews
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        
        int overdueCount = 0;
        int completedCount = 0;
        int totalScheduledOrCompleted = 0;
        
        List<Map<String, Object>> reviewsTableData = departmentReviews.stream().map(review -> {
            Map<String, Object> row = new HashMap<>();
            
            row.put("id", review.getId());
            row.put("employeeName", review.getEmployee().getUser().getFullName());
            row.put("employeeTitle", review.getEmployee().getPosition() != null ? review.getEmployee().getPosition().getName() : "Employee");
            row.put("avatarUrl", review.getEmployee().getUser().getAvatarUrl());
            
            row.put("cyclePeriod", review.getReviewPeriod());
            row.put("reviewDate", review.getUpdatedAt() != null ? review.getUpdatedAt().format(dateFormatter) : "N/A");
            
            String status = review.getStatus();
            row.put("status", status);
            
            // Map status logic for filtering flags
            switch(status.toUpperCase()) {
                case "COMPLETED":
                    row.put("statusTheme", "bg-emerald-50 text-emerald-600 border-emerald-100");
                    break;
                case "SCHEDULED":
                    row.put("statusTheme", "bg-blue-50 text-blue-600 border-blue-100");
                    break;
                case "OVERDUE":
                case "DRAFT":
                default:
                    // Usually logic to determine if overdue is date based, but since the schema has "status", we rely on that.
                    status = "OVERDUE";
                    row.put("status", status); // Force Overdue visual
                    row.put("statusTheme", "bg-rose-50 text-rose-600 border-rose-100");
                    break;
            }
            return row;
        }).collect(Collectors.toList());

        // Process overall metrics
        for (PerformanceReview pr : departmentReviews) {
            String status = pr.getStatus() != null ? pr.getStatus().toUpperCase() : "DRAFT";
            if (status.equals("COMPLETED")) {
                completedCount++;
                totalScheduledOrCompleted++;
            } else if (status.equals("SCHEDULED")) {
                totalScheduledOrCompleted++;
            } else {
                overdueCount++;
            }
        }
        
        int completionRate = 0;
        if (totalScheduledOrCompleted > 0) {
           completionRate = (int) Math.round((double) completedCount / totalScheduledOrCompleted * 100);
        }

        // Mock growth score since we need historical query
        String avgScoreGrowth = "+8%";

        data.put("reviews", reviewsTableData);
        data.put("overdueCount", overdueCount);
        data.put("completionRate", completionRate + "%");
        data.put("averageGrowth", avgScoreGrowth);

        return data;
    }
}
