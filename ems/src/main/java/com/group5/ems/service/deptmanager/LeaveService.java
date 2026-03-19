package com.group5.ems.service.deptmanager;

import com.group5.ems.entity.Department;
import com.group5.ems.entity.User;
import com.group5.ems.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveService {

    private final RequestRepository requestRepository;
    private final DeptManagerUtilService utilService;

    public Map<String, Object> getLeaveApprovalData() {
        Map<String, Object> data = new HashMap<>();
        User currentUser = utilService.getCurrentUser();
        Map<String, String> managerMap = utilService.getManagerMap(currentUser);
        data.put("manager", managerMap);

        Department dept = utilService.getDepartmentForManager(currentUser);
        List<com.group5.ems.entity.Request> requests = new ArrayList<>();
        if (dept != null) {
            requests = requestRepository.findByEmployeeDepartmentIdAndLeaveTypeIsNotNullOrderByCreatedAtDesc(dept.getId());
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.YearMonth currentMonth = java.time.YearMonth.now();

        int pendingApprovals = 0;
        int approvedThisMonth = 0;
        int awayToday = 0;

        List<Map<String, Object>> mappedRequests = new ArrayList<>();

        for (com.group5.ems.entity.Request req : requests) {
            // Metrics
            if ("PENDING".equalsIgnoreCase(req.getStatus())) {
                pendingApprovals++;
            } else if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
                if (req.getApprovedAt() != null && java.time.YearMonth.from(req.getApprovedAt()).equals(currentMonth)) {
                    approvedThisMonth++;
                }
                if (req.getLeaveFrom() != null && req.getLeaveTo() != null) {
                    if (!today.isBefore(req.getLeaveFrom()) && !today.isAfter(req.getLeaveTo())) {
                        awayToday++;
                    }
                }
            }

            // Map to view data
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("id", req.getId());

            Map<String, String> empMap = new HashMap<>();
            if (req.getEmployee() != null) {
                empMap.put("name", req.getEmployee().getUser() != null ? req.getEmployee().getUser().getFullName() : "Unknown");
                empMap.put("role", req.getEmployee().getPosition() != null ? req.getEmployee().getPosition().getName() : "Employee");
                empMap.put("avatarUrl", req.getEmployee().getUser() != null && req.getEmployee().getUser().getAvatarUrl() != null
                        ? req.getEmployee().getUser().getAvatarUrl()
                        : "https://lh3.googleusercontent.com/aida-public/AB6AXuAx3bm_6ROku45Qad2UC6L8WqGYQTSxbQfGbrIsZyy-UW0G-0eeaUe05OzGGUPVXtUgSAXYY1km4lsQ8OMlKocQqnLvoWylgqv8HhjdOhc-kA7_Y9WGXOHncHiVIom2GDXi5UFfTRWNw-kIM5Tj5rLVJx3alhzAv1liLktNE8Zt65-kYJuInGPkWm85aD_STgeoCKnakLN1ZpxNfG-GLOhHh26_zxMgT8NQ21STEfw2DrFNb7ygWY6IQKmzRFuP-NmzVNfiEHO9zvA");
            }
            reqMap.put("employee", empMap);

            // Friendly formatting
            String typeStr = req.getLeaveType() != null ? req.getLeaveType().replace("_", " ").toLowerCase() : "";
            if (typeStr.contains("annual")) {
                reqMap.put("typeDisplay", "Annual Leave");
                reqMap.put("typeColorClass", "bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-300 border-blue-100");
                reqMap.put("typeDotClass", "bg-blue-500");
            } else if (typeStr.contains("sick")) {
                reqMap.put("typeDisplay", "Sick Leave");
                reqMap.put("typeColorClass", "bg-rose-50 dark:bg-rose-900/30 text-rose-600 dark:text-rose-300 border-rose-100");
                reqMap.put("typeDotClass", "bg-rose-500");
            } else {
                reqMap.put("typeDisplay", req.getLeaveType());
                reqMap.put("typeColorClass", "bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 border-purple-100");
                reqMap.put("typeDotClass", "bg-purple-500");
            }

            long durationDays = 0;
            if (req.getLeaveFrom() != null && req.getLeaveTo() != null) {
                durationDays = java.time.temporal.ChronoUnit.DAYS.between(req.getLeaveFrom(), req.getLeaveTo()) + 1;
            }
            reqMap.put("durationDays", durationDays);

            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("MMM dd");
            String dates = "";
            if (req.getLeaveFrom() != null) {
                dates += req.getLeaveFrom().format(dtf);
                if (req.getLeaveTo() != null && !req.getLeaveTo().equals(req.getLeaveFrom())) {
                    dates += " - " + req.getLeaveTo().format(dtf);
                }
            }
            reqMap.put("dates", dates);
            reqMap.put("appliedOn", req.getCreatedAt() != null ? req.getCreatedAt().format(dtf) : "N/A");

            reqMap.put("status", req.getStatus());
            if ("PENDING".equalsIgnoreCase(req.getStatus())) {
                reqMap.put("statusClass", "bg-amber-50 dark:bg-amber-900/20 text-amber-600 border-amber-100");
                reqMap.put("statusDisplay", "Pending Review");
            } else if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
                reqMap.put("statusClass", "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 border-emerald-100");
                reqMap.put("statusDisplay", "Approved");
            } else {
                reqMap.put("statusClass", "bg-rose-50 dark:bg-rose-900/20 text-rose-600 border-rose-100");
                reqMap.put("statusDisplay", "Rejected");
            }

            mappedRequests.add(reqMap);
        }

        data.put("pendingApprovals", pendingApprovals);
        data.put("approvedThisMonth", approvedThisMonth);
        data.put("awayToday", awayToday);
        data.put("requests", mappedRequests);

        return data;
    }

    @Transactional
    public void approveLeaveRequest(Long requestId) {
        requestRepository.findById(requestId).ifPresent(req -> {
            req.setStatus("APPROVED");
            req.setApprovedAt(java.time.LocalDateTime.now());
            User currentUser = utilService.getCurrentUser();
            if (currentUser != null) {
                req.setApprovedBy(currentUser.getId());
            }
            requestRepository.save(req);
        });
    }

    @Transactional
    public void rejectLeaveRequest(Long requestId) {
        requestRepository.findById(requestId).ifPresent(req -> {
            req.setStatus("REJECTED");
            User currentUser = utilService.getCurrentUser();
            if (currentUser != null) {
                req.setApprovedBy(currentUser.getId());
            }
            requestRepository.save(req);
        });
    }
}
