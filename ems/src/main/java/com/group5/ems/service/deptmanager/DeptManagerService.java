package com.group5.ems.service.deptmanager;

import com.group5.ems.entity.Attendance;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.PerformanceReview;
import com.group5.ems.entity.Position;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.RequestApprovalHistory;
import com.group5.ems.entity.RequestType;
import com.group5.ems.entity.Salary;
import com.group5.ems.entity.StaffingRequest;
import com.group5.ems.entity.User;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.AttendanceRepository;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.PerformanceReviewRepository;
import com.group5.ems.repository.PositionRepository;
import com.group5.ems.repository.RequestApprovalHistoryRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.RequestTypeRepository;
import com.group5.ems.repository.SalaryRepository;
import com.group5.ems.repository.StaffingRequestRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeptManagerService {

    private static final String DEFAULT_AVATAR =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAx3bm_6ROku45Qad2UC6L8WqGYQTSxbQfGbrIsZyy-UW0G-0eeaUe05OzGGUPVXtUgSAXYY1km4lsQ8OMlKocQqnLvoWylgqv8HhjdOhc-kA7_Y9WGXOHncHiVIom2GDXi5UFfTRWNw-kIM5Tj5rLVJx3alhzAv1liLktNE8Zt65-kYJuInGPkWm85aD_STgeoCKnakLN1ZpxNfG-GLOhHh26_zxMgT8NQ21STEfw2DrFNb7ygWY6IQKmzRFuP-NmzVNfiEHO9zvA";
    private static final LocalTime LATE_AFTER = LocalTime.of(9, 0);
    private static final LocalTime ABSENT_AFTER = LocalTime.of(10, 30);

    private final DepartmentRepository departmentRepository;
    private final DeptManagerUtilService utilService;
    private final RequestRepository requestRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final PositionRepository positionRepository;
    private final AttendanceRepository attendanceRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final SalaryRepository salaryRepository;
    private final RequestApprovalHistoryRepository requestApprovalHistoryRepository;
    private final StaffingRequestRepository staffingRequestRepository;
    private final LogService logService;

    public int getTeamSize(Long managerId) {
        return departmentRepository.findByManagerId(managerId).size();
    }

    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        User currentUser = utilService.getCurrentUser();
        Department department = utilService.getDepartmentForManager(currentUser);

        data.put("manager", utilService.getManagerMap(currentUser));

        if (department == null) {
            data.put("pendingApprovals", 0);
            data.put("newApprovals", 0);
            data.put("teamSize", 0);
            data.put("activeCount", 0);
            data.put("inactiveCount", 0);
            data.put("suspendedCount", 0);
            data.put("teamAttendance", "0%");
            data.put("nextReview", "No department assigned");
            data.put("recentTeamActivities", List.of());
            data.put("actionItems", List.of());
            data.put("statusBreakdown", List.of());
            return data;
        }

        List<Employee> employees = safeEmployees(department);
        Map<Long, PerformanceReview> latestReviewByEmployee = getLatestReviewByEmployee(employees);
        Map<Long, LocalDateTime> latestTouchPointByEmployee = buildLatestTouchPointByEmployee(employees, latestReviewByEmployee);
        Map<Long, String> weeklyAttendanceByEmployee = getWeeklyAttendanceByEmployee(employees);
        List<PerformanceReview> departmentReviews = performanceReviewRepository
                .findByEmployee_DepartmentIdOrderByUpdatedAtDesc(department.getId());

        int pendingApprovals = utilService.getPendingApprovalsCount(department);
        int activeCount = 0;
        int inactiveCount = 0;
        int suspendedCount = 0;

        for (Employee employee : employees) {
            String status = employee.getStatus() != null ? employee.getStatus().toUpperCase(Locale.ROOT) : "ACTIVE";
            if ("ACTIVE".equals(status)) {
                activeCount++;
            } else if ("ON_LEAVE".equals(status) || "INACTIVE".equals(status)) {
                inactiveCount++;
            } else {
                suspendedCount++;
            }
        }

        data.put("pendingApprovals", pendingApprovals);
        data.put("newApprovals", pendingApprovals);
        data.put("teamSize", employees.size());
        data.put("activeCount", activeCount);
        data.put("inactiveCount", inactiveCount);
        data.put("suspendedCount", suspendedCount);
        data.put("teamAttendance", calculateTeamAttendance(employees));
        data.put("nextReview", determineNextReviewLabel(employees, latestReviewByEmployee));
        data.put("recentTeamActivities", buildRecentTeamActivities(employees, latestReviewByEmployee, weeklyAttendanceByEmployee, latestTouchPointByEmployee));
        data.put("actionItems", buildDashboardActionItems(department, employees, latestReviewByEmployee));
        data.put("statusBreakdown", buildStatusBreakdown(activeCount, inactiveCount, suspendedCount));
        data.put("performanceTrend", buildPerformanceTrend(departmentReviews));

        return data;
    }

    public Map<String, Object> getTeamData() {
        Map<String, Object> data = new HashMap<>();
        User currentUser = utilService.getCurrentUser();
        Department department = utilService.getDepartmentForManager(currentUser);

        data.put("manager", utilService.getManagerMap(currentUser));

        if (department == null) {
            data.put("pendingApprovals", 0);
            data.put("newApprovals", 0);
            data.put("teamMembers", List.of());
            data.put("allPositions", List.of());
            data.put("topPerformers", 0);
            data.put("onLeaveCount", 0);
            data.put("recentHires", 0);
            return data;
        }

        int pendingApprovals = utilService.getPendingApprovalsCount(department);
        List<Employee> employees = safeEmployees(department);
        Map<Long, PerformanceReview> latestReviewByEmployee = getLatestReviewByEmployee(employees);

        List<Map<String, String>> members = new ArrayList<>();
        int topPerformers = 0;
        int onLeaveCount = 0;
        int recentHires = 0;
        LocalDate recentHireThreshold = LocalDate.now().minusDays(90);

        for (Employee employee : employees) {
            User employeeUser = employee.getUser();
            PerformanceReview latestReview = latestReviewByEmployee.get(employee.getId());
            String rating = latestReview != null && latestReview.getPerformanceScore() != null
                    ? latestReview.getPerformanceScore().stripTrailingZeros().toPlainString()
                    : "No review";

            if (latestReview != null && latestReview.getPerformanceScore() != null
                    && latestReview.getPerformanceScore().compareTo(BigDecimal.valueOf(4)) >= 0) {
                topPerformers++;
            }

            if ("ON_LEAVE".equalsIgnoreCase(employee.getStatus())) {
                onLeaveCount++;
            }

            if (employee.getHireDate() != null && !employee.getHireDate().isBefore(recentHireThreshold)) {
                recentHires++;
            }

            Map<String, String> member = new HashMap<>();
            member.put("id", String.valueOf(employee.getId()));
            member.put("empCode", employee.getEmployeeCode() != null
                    ? employee.getEmployeeCode()
                    : "EMP-" + String.format("%03d", employee.getId()));
            member.put("name", employeeUser != null ? employeeUser.getFullName() : "Employee #" + employee.getId());
            member.put("email", employeeUser != null ? employeeUser.getEmail() : "");
            member.put("role", employee.getPosition() != null ? employee.getPosition().getName() : "Staff");
            member.put("rating", rating);
            member.put("ratingClass", ratingClass(latestReview));
            member.put("status", employee.getStatus() != null ? employee.getStatus() : "ACTIVE");
            member.put("statusDot", "ACTIVE".equalsIgnoreCase(employee.getStatus()) ? "bg-green-500" : "bg-amber-500");
            member.put("avatarUrl", avatarOf(employeeUser));
            members.add(member);
        }

        List<Map<String, String>> positionList = positionRepository.findByDepartmentId(department.getId()).stream()
                .sorted(Comparator.comparing(Position::getName, String.CASE_INSENSITIVE_ORDER))
                .map(position -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("id", String.valueOf(position.getId()));
                    item.put("name", position.getName());
                    return item;
                })
                .collect(Collectors.toList());

        data.put("pendingApprovals", pendingApprovals);
        data.put("newApprovals", pendingApprovals);
        data.put("teamMembers", members);
        data.put("allPositions", positionList);
        data.put("topPerformers", topPerformers);
        data.put("onLeaveCount", onLeaveCount);
        data.put("recentHires", recentHires);

        return data;
    }

    public Map<String, Object> getDepartmentData() {
        Map<String, Object> data = new HashMap<>();
        User currentUser = utilService.getCurrentUser();
        Map<String, String> managerMap = utilService.getManagerMap(currentUser);
        Department department = utilService.getDepartmentForManager(currentUser);

        data.put("manager", managerMap);
        data.put("pendingApprovals", utilService.getPendingApprovalsCount(department));
        data.put("newApprovals", utilService.getPendingApprovalsCount(department));

        Map<String, String> departmentMap = new HashMap<>();
        List<Map<String, String>> teams = new ArrayList<>();
        List<Map<String, String>> positions = new ArrayList<>();
        List<Map<String, String>> staffingUpdates = new ArrayList<>();

        if (department == null) {
            departmentMap.put("name", "No Department Assigned");
            departmentMap.put("code", "N/A");
            departmentMap.put("description", "You are not currently managing any departments.");
            departmentMap.put("manager", managerMap.get("name"));
            departmentMap.put("totalEmployees", "0");
            departmentMap.put("openPositions", "0");
            departmentMap.put("currentPayroll", "$0.00");
            data.put("department", departmentMap);
            data.put("teams", teams);
            data.put("positions", positions);
            data.put("staffingUpdates", staffingUpdates);
            return data;
        }

        List<Employee> employees = safeEmployees(department);
        List<Position> departmentPositions = positionRepository.findByDepartmentId(department.getId());
        Map<Long, Long> headcountByPositionId = employees.stream()
                .filter(employee -> employee.getPositionId() != null)
                .collect(Collectors.groupingBy(Employee::getPositionId, Collectors.counting()));

        departmentMap.put("name", department.getName() != null ? department.getName() : "Unnamed Department");
        departmentMap.put("code", department.getCode() != null ? department.getCode() : "N/A");
        departmentMap.put("description", department.getDescription() != null
                ? department.getDescription()
                : "Department operations run here.");
        departmentMap.put("manager", managerMap.get("name"));
        departmentMap.put("totalEmployees", String.valueOf(employees.size()));
        departmentMap.put("openPositions", String.valueOf(countOpenPositions(departmentPositions, headcountByPositionId)));
        departmentMap.put("currentPayroll", formatCurrency(calculateDepartmentPayroll(employees)));

        for (Department child : department.getChildren()) {
            Map<String, String> team = new HashMap<>();
            team.put("name", child.getName());
            team.put("headcount", String.valueOf(safeEmployees(child).size()));
            String leadName = "None";
            if (child.getManager() != null && child.getManager().getUser() != null) {
                leadName = child.getManager().getUser().getFullName();
            }
            team.put("lead", leadName);
            teams.add(team);
        }

        for (Position position : departmentPositions) {
            long headcount = headcountByPositionId.getOrDefault(position.getId(), 0L);

            Map<String, String> positionMap = new HashMap<>();
            positionMap.put("title", position.getName());
            positionMap.put("headcount", String.valueOf(headcount));
            positionMap.put("status", headcount > 0 ? "Filled" : "Open");
            positionMap.put("statusClass", headcount > 0
                    ? "bg-green-100 text-green-700"
                    : "bg-amber-100 text-amber-700");
            positions.add(positionMap);
        }

        List<Map<String, String>> combinedUpdates = new ArrayList<>();

        staffingRequestRepository.findRecentByDepartmentId(department.getId()).stream()
                .map(this::mapStaffingUpdate)
                .forEach(combinedUpdates::add);

        requestRepository.findDepartmentWorkflowRequestsByTypeCode(department.getId(), "HR_REMOVAL").stream()
                .map(this::mapRemovalUpdate)
                .forEach(combinedUpdates::add);

        staffingUpdates = combinedUpdates.stream()
                .sorted(Comparator.comparing(
                        (Map<String, String> item) -> !"Pending".equalsIgnoreCase(item.get("status"))
                ).thenComparing(
                        item -> LocalDateTime.parse(item.get("sortAt")),
                        Comparator.reverseOrder()
                ))
                .map(item -> {
                    item.remove("sortAt");
                    return item;
                })
                .collect(Collectors.toList());

        data.put("department", departmentMap);
        data.put("teams", teams);
        data.put("positions", positions);
        data.put("staffingUpdates", staffingUpdates);

        return data;
    }

    @Transactional
    public boolean createRemovalRequest(Long employeeId, String reason) {
        Department department = utilService.getCurrentManagedDepartment();
        Employee managerEmployee = utilService.getCurrentEmployee();
        if (department == null || managerEmployee == null || !utilService.isEmployeeInManagedDepartment(employeeId)) {
            return false;
        }

        Employee targetEmployee = department.getEmployees().stream()
                .filter(employee -> employeeId.equals(employee.getId()))
                .findFirst()
                .orElse(null);

        RequestType requestType = findOrCreateRequestType(
                "HR_REMOVAL",
                "Member Removal Request",
                "HR_STATUS",
                "Request to remove a department member"
        );

        Request request = new Request();
        request.setEmployeeId(managerEmployee.getId());
        request.setRequestTypeId(requestType.getId());
        request.setTitle("Removal request for " + displayEmployeeName(targetEmployee));
        request.setContent(buildRemovalRequestContent(department, targetEmployee, reason));
        request.setStatus("PENDING");
        Request savedRequest = requestRepository.save(request);
        saveHistory(savedRequest.getId(), managerEmployee.getUserId(), "SUBMITTED", "Submitted by Department Manager");
        logService.log(AuditAction.CREATE, AuditEntityType.REQUEST, savedRequest.getId(), managerEmployee.getUserId());
        return true;
    }

    @Transactional
    public boolean createAddMemberRequest(String requestType, String role, String description) {
        Department department = utilService.getCurrentManagedDepartment();
        Employee managerEmployee = utilService.getCurrentEmployee();
        if (department == null || managerEmployee == null) {
            return false;
        }

        String normalizedType = requestType != null ? requestType.trim().toUpperCase(Locale.ROOT) : "RECRUITMENT";

        // Create StaffingRequest instead of Request
        StaffingRequest staffingRequest = new StaffingRequest();
        staffingRequest.setDepartmentId(department.getId());
        staffingRequest.setRequestedByEmployeeId(managerEmployee.getId());
        staffingRequest.setRequestType(normalizedType);
        staffingRequest.setRoleRequested(role);
        staffingRequest.setDescription(description);
        staffingRequest.setStatus("PENDING");
        
        StaffingRequest savedRequest = staffingRequestRepository.save(staffingRequest);
        logService.log(AuditAction.CREATE, AuditEntityType.REQUEST, savedRequest.getId(), managerEmployee.getUserId());
        
        return true;
    }

    @Transactional
    public boolean cancelStaffingRequest(Long requestId) {
        Department department = utilService.getCurrentManagedDepartment();
        Employee managerEmployee = utilService.getCurrentEmployee();
        User currentUser = utilService.getCurrentUser();
        if (department == null || managerEmployee == null || requestId == null) {
            return false;
        }

        StaffingRequest request = staffingRequestRepository.findById(requestId).orElse(null);
        if (request == null
                || !department.getId().equals(request.getDepartmentId())
                || !managerEmployee.getId().equals(request.getRequestedByEmployeeId())
                || !"PENDING".equalsIgnoreCase(request.getStatus())) {
            return false;
        }

        request.setStatus("CANCELLED");
        request.setProcessedByUserId(currentUser.getId());
        request.setProcessedAt(LocalDateTime.now());
        staffingRequestRepository.save(request);
        logService.log(AuditAction.UPDATE, AuditEntityType.REQUEST, request.getId(), currentUser.getId());
        return true;
    }

    @Transactional
    public boolean cancelRemovalRequest(Long requestId) {
        Department department = utilService.getCurrentManagedDepartment();
        Employee managerEmployee = utilService.getCurrentEmployee();
        User currentUser = utilService.getCurrentUser();
        if (department == null || managerEmployee == null || requestId == null) {
            return false;
        }

        Request request = requestRepository.findById(requestId).orElse(null);
        if (request == null
                || request.getEmployee() == null
                || !department.getId().equals(request.getEmployee().getDepartmentId())
                || !managerEmployee.getId().equals(request.getEmployeeId())
                || request.getRequestType() == null
                || !"HR_REMOVAL".equalsIgnoreCase(request.getRequestType().getCode())
                || !"PENDING".equalsIgnoreCase(request.getStatus())) {
            return false;
        }

        request.setStatus("CANCELLED");
        requestRepository.save(request);
        saveHistory(request.getId(), currentUser.getId(), "CANCELLED", "Cancelled by Department Manager");
        logService.log(AuditAction.UPDATE, AuditEntityType.REQUEST, request.getId(), currentUser.getId());
        return true;
    }

    private List<Employee> safeEmployees(Department department) {
        return department != null && department.getEmployees() != null ? department.getEmployees() : List.of();
    }

    private String avatarOf(User user) {
        return user != null && user.getAvatarUrl() != null ? user.getAvatarUrl() : DEFAULT_AVATAR;
    }

    private Map<Long, PerformanceReview> getLatestReviewByEmployee(List<Employee> employees) {
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, PerformanceReview> latest = new LinkedHashMap<>();
        for (PerformanceReview review : performanceReviewRepository.findByEmployeeIdInOrderByUpdatedAtDesc(employeeIds)) {
            latest.putIfAbsent(review.getEmployeeId(), review);
        }
        return latest;
    }

    private Map<Long, LocalDateTime> buildLatestTouchPointByEmployee(List<Employee> employees,
                                                                     Map<Long, PerformanceReview> latestReviewByEmployee) {
        Map<Long, LocalDateTime> latestTouchPoints = new HashMap<>();
        for (Employee employee : employees) {
            PerformanceReview review = latestReviewByEmployee.get(employee.getId());
            LocalDateTime reviewTouchPoint = review != null
                    ? (review.getUpdatedAt() != null ? review.getUpdatedAt() : review.getCreatedAt())
                    : null;
            LocalDateTime employeeTouchPoint = employee.getUpdatedAt() != null ? employee.getUpdatedAt() : employee.getCreatedAt();
            latestTouchPoints.put(employee.getId(), reviewTouchPoint != null ? reviewTouchPoint : employeeTouchPoint);
        }
        return latestTouchPoints;
    }

    private Map<Long, String> getWeeklyAttendanceByEmployee(List<Employee> employees) {
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        LocalDate today = LocalDate.now();

        Map<Long, List<Attendance>> grouped = attendanceRepository
                .findByEmployeeIdInAndWorkDateBetweenOrderByWorkDateAsc(employeeIds, monday, sunday)
                .stream()
                .collect(Collectors.groupingBy(Attendance::getEmployeeId));
        Map<Long, Set<LocalDate>> approvedLeaveDays = buildApprovedLeaveDays(employeeIds, monday, sunday);

        Map<Long, String> attendanceSummary = new HashMap<>();
        for (Employee employee : employees) {
            List<Attendance> attendances = grouped.getOrDefault(employee.getId(), List.of());
            Set<LocalDate> leaveDays = approvedLeaveDays.getOrDefault(employee.getId(), Set.of());
            long present = attendances.stream()
                    .filter(att -> "PRESENT".equalsIgnoreCase(resolveDisplayStatus(att, leaveDays.contains(att.getWorkDate()))))
                    .count();
            long late = attendances.stream()
                    .filter(att -> "LATE".equalsIgnoreCase(resolveDisplayStatus(att, leaveDays.contains(att.getWorkDate()))))
                    .count();
            long absent = attendances.stream()
                    .filter(att -> "ABSENT".equalsIgnoreCase(resolveDisplayStatus(att, leaveDays.contains(att.getWorkDate()))))
                    .count();
            long expectedWorkingDays = countExpectedWorkingDays(monday, sunday, today);
            long missingAbsences = Math.max(0, expectedWorkingDays - attendances.size() - leaveDays.size());
            absent += missingAbsences;

            if (absent > 0) {
                attendanceSummary.put(employee.getId(), "Absence (" + absent + ")");
            } else if (late > 0) {
                attendanceSummary.put(employee.getId(), "Late (" + late + ")");
            } else if (!leaveDays.isEmpty()) {
                attendanceSummary.put(employee.getId(), "On Leave (" + leaveDays.size() + ")");
            } else if (present > 0) {
                attendanceSummary.put(employee.getId(), "On track");
            } else {
                attendanceSummary.put(employee.getId(), "No data");
            }
        }
        return attendanceSummary;
    }

    private String calculateTeamAttendance(List<Employee> employees) {
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
        if (employeeIds.isEmpty()) {
            return "0%";
        }

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        LocalDate today = LocalDate.now();
        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdInAndWorkDateBetweenOrderByWorkDateAsc(employeeIds, monday, sunday);
        Map<Long, Set<LocalDate>> approvedLeaveDays = buildApprovedLeaveDays(employeeIds, monday, sunday);

        long accountableRecords = attendances.stream()
                .map(att -> resolveDisplayStatus(att, approvedLeaveDays.getOrDefault(att.getEmployeeId(), Set.of()).contains(att.getWorkDate())))
                .filter(status -> "PRESENT".equalsIgnoreCase(status)
                        || "LATE".equalsIgnoreCase(status)
                        || "ABSENT".equalsIgnoreCase(status)
                        || "LEAVE".equalsIgnoreCase(status))
                .count();
        long attended = attendances.stream()
                .map(att -> resolveDisplayStatus(att, approvedLeaveDays.getOrDefault(att.getEmployeeId(), Set.of()).contains(att.getWorkDate())))
                .filter(status -> "PRESENT".equalsIgnoreCase(status)
                        || "LATE".equalsIgnoreCase(status)
                        || "LEAVE".equalsIgnoreCase(status))
                .count();
        long approvedLeaveCount = approvedLeaveDays.values().stream().mapToLong(Set::size).sum();
        attended = Math.max(attended, approvedLeaveCount);

        long expectedWorkingDays = countExpectedWorkingDays(monday, sunday, today) * employeeIds.size();
        long finalAccountableRecords = Math.max(accountableRecords, expectedWorkingDays);

        if (finalAccountableRecords == 0) {
            return "0%";
        }

        long percentage = Math.round((double) attended * 100 / finalAccountableRecords);
        return percentage + "%";
    }

    private String determineNextReviewLabel(List<Employee> employees, Map<Long, PerformanceReview> latestReviewByEmployee) {
        return employees.stream()
                .map(employee -> latestReviewByEmployee.get(employee.getId()))
                .filter(review -> review != null && !"COMPLETED".equalsIgnoreCase(review.getStatus()))
                .min(Comparator.comparing(review -> review.getUpdatedAt() != null ? review.getUpdatedAt() : review.getCreatedAt()))
                .map(review -> review.getReviewPeriod() != null ? review.getReviewPeriod() : "Pending review")
                .orElse("No pending review");
    }

    private List<Map<String, String>> buildRecentTeamActivities(List<Employee> employees,
                                                                Map<Long, PerformanceReview> latestReviewByEmployee,
                                                                Map<Long, String> weeklyAttendanceByEmployee,
                                                                Map<Long, LocalDateTime> latestTouchPointByEmployee) {
        return employees.stream()
                .sorted(Comparator.comparing(
                        (Employee employee) -> latestTouchPointByEmployee.get(employee.getId()),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .limit(5)
                .map(employee -> {
                    User employeeUser = employee.getUser();
                    PerformanceReview latestReview = latestReviewByEmployee.get(employee.getId());

                    Map<String, String> item = new HashMap<>();
                    item.put("name", employeeUser != null ? employeeUser.getFullName() : "Employee #" + employee.getId());
                    item.put("title", employee.getPosition() != null ? employee.getPosition().getName() : "Staff");
                    item.put("avatarUrl", avatarOf(employeeUser));
                    item.put("status", employee.getStatus() != null ? employee.getStatus() : "ACTIVE");
                    item.put("statusClass", "ACTIVE".equalsIgnoreCase(employee.getStatus())
                            ? "bg-green-100 text-green-800"
                            : "bg-amber-100 text-amber-800");
                    item.put("attendance", weeklyAttendanceByEmployee.getOrDefault(employee.getId(), "No data"));
                    item.put("lastReview", latestReview != null
                            ? (latestReview.getReviewPeriod() + " - " + latestReview.getStatus())
                            : "No review");
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, String>> buildDashboardActionItems(Department department,
                                                                List<Employee> employees,
                                                                Map<Long, PerformanceReview> latestReviewByEmployee) {
        List<Map<String, String>> items = new ArrayList<>();

        requestRepository.findByEmployeeDepartmentIdAndLeaveTypeIsNotNullOrderByCreatedAtDesc(department.getId()).stream()
                .filter(request -> "PENDING".equalsIgnoreCase(request.getStatus()))
                .limit(2)
                .forEach(request -> items.add(actionItem(
                        "Leave approval pending",
                        request.getEmployee() != null && request.getEmployee().getUser() != null
                                ? request.getEmployee().getUser().getFullName() + " submitted " + request.getLeaveType()
                                : "A team member submitted a leave request",
                        "/dept-manager/leave-approval"
                )));

        employees.stream()
                .filter(employee -> {
                    PerformanceReview review = latestReviewByEmployee.get(employee.getId());
                    return review == null || !"COMPLETED".equalsIgnoreCase(review.getStatus());
                })
                .limit(2)
                .forEach(employee -> items.add(actionItem(
                        "Performance review needs attention",
                        displayEmployeeName(employee) + " still has a pending review cycle",
                        "/dept-manager/performance-review"
                )));

        if (items.isEmpty()) {
            items.add(actionItem("No immediate blockers", "Your department does not have pending review tasks right now.", "/dept-manager/my-team"));
        }

        return items;
    }

    private List<Map<String, String>> buildStatusBreakdown(int activeCount, int inactiveCount, int suspendedCount) {
        List<Map<String, String>> items = new ArrayList<>();
        int total = activeCount + inactiveCount + suspendedCount;
        items.add(statusItem("Active", activeCount, total, "bg-green-500"));
        items.add(statusItem("On Leave / Inactive", inactiveCount, total, "bg-amber-500"));
        items.add(statusItem("Other", suspendedCount, total, "bg-slate-500"));
        return items;
    }

    private Map<String, String> statusItem(String label, int value, int total, String colorClass) {
        Map<String, String> item = new HashMap<>();
        item.put("label", label);
        item.put("value", String.valueOf(value));
        item.put("percent", total > 0 ? String.valueOf(Math.max(6, Math.round((double) value * 100 / total))) : "0");
        item.put("colorClass", colorClass);
        return item;
    }

    private String resolveAttendanceStatus(Attendance attendance) {
        if (attendance == null) {
            return "ABSENT";
        }
        if (attendance.getCheckIn() != null) {
            return resolveStatusByCheckIn(attendance.getCheckIn());
        }
        return attendance.getStatus() != null ? attendance.getStatus() : "ABSENT";
    }

    private String resolveStatusByCheckIn(LocalTime checkIn) {
        if (checkIn == null) {
            return "ABSENT";
        }
        if (checkIn.isAfter(ABSENT_AFTER)) {
            return "ABSENT";
        }
        if (checkIn.isAfter(LATE_AFTER)) {
            return "LATE";
        }
        return "PRESENT";
    }

    private String resolveDisplayStatus(Attendance attendance, boolean approvedLeaveDay) {
        if (!approvedLeaveDay) {
            return resolveAttendanceStatus(attendance);
        }
        if (attendance != null && attendance.getCheckIn() != null) {
            return "PRESENT";
        }
        return "LEAVE";
    }

    private long countExpectedWorkingDays(LocalDate monday, LocalDate sunday, LocalDate today) {
        LocalDate effectiveEnd = sunday.isBefore(today) ? sunday : today;
        if (effectiveEnd.isBefore(monday)) {
            return 0;
        }

        long count = 0;
        LocalDate cursor = monday;
        while (!cursor.isAfter(effectiveEnd)) {
            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }

    private Map<Long, Set<LocalDate>> buildApprovedLeaveDays(List<Long> employeeIds, LocalDate rangeStart, LocalDate rangeEnd) {
        Map<Long, Set<LocalDate>> leaveDays = new HashMap<>();
        if (employeeIds.isEmpty()) {
            return leaveDays;
        }

        List<Request> approvedLeaves = requestRepository.findApprovedLeaveRequestsByEmployeeIdsAndDateRange(employeeIds, rangeStart, rangeEnd);
        for (Request leave : approvedLeaves) {
            LocalDate start = leave.getLeaveFrom().isBefore(rangeStart) ? rangeStart : leave.getLeaveFrom();
            LocalDate end = leave.getLeaveTo().isAfter(rangeEnd) ? rangeEnd : leave.getLeaveTo();
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                DayOfWeek dayOfWeek = cursor.getDayOfWeek();
                if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                    leaveDays.computeIfAbsent(leave.getEmployeeId(), key -> new HashSet<>()).add(cursor);
                }
                cursor = cursor.plusDays(1);
            }
        }
        return leaveDays;
    }

    private Map<String, Object> buildPerformanceTrend(List<PerformanceReview> reviews) {
        Map<YearMonth, List<BigDecimal>> monthlyScores = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            monthlyScores.put(currentMonth.minusMonths(i), new ArrayList<>());
        }

        for (PerformanceReview review : reviews) {
            if (review.getPerformanceScore() == null) {
                continue;
            }
            LocalDateTime timestamp = review.getUpdatedAt() != null ? review.getUpdatedAt() : review.getCreatedAt();
            if (timestamp == null) {
                continue;
            }
            YearMonth month = YearMonth.from(timestamp);
            if (monthlyScores.containsKey(month)) {
                monthlyScores.get(month).add(review.getPerformanceScore());
            }
        }

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        List<Map<String, Object>> points = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int countedMonths = 0;
        BigDecimal firstValue = null;
        BigDecimal lastValue = null;

        for (Map.Entry<YearMonth, List<BigDecimal>> entry : monthlyScores.entrySet()) {
            BigDecimal average = averageScores(entry.getValue());
            if (average != null) {
                total = total.add(average);
                countedMonths++;
                if (firstValue == null) {
                    firstValue = average;
                }
                lastValue = average;
            }

            Map<String, Object> point = new HashMap<>();
            point.put("label", entry.getKey().format(monthFormatter).toUpperCase(Locale.ENGLISH));
            point.put("value", average != null ? average : BigDecimal.ZERO);
            points.add(point);
        }

        BigDecimal averageScore = countedMonths > 0
                ? total.divide(BigDecimal.valueOf(countedMonths), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal changePercent = BigDecimal.ZERO;
        if (firstValue != null && lastValue != null && firstValue.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = lastValue.subtract(firstValue)
                    .divide(firstValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("points", points);
        trend.put("averageScore", averageScore.setScale(0, RoundingMode.HALF_UP).toPlainString());
        trend.put("averagePercent", averageScore.multiply(BigDecimal.valueOf(20)).setScale(0, RoundingMode.HALF_UP).toPlainString() + "%");
        trend.put("changeText", formatSignedPercent(changePercent));
        trend.put("changePositive", changePercent.compareTo(BigDecimal.ZERO) >= 0);
        return trend;
    }

    private BigDecimal averageScores(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) {
            return null;
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private String formatSignedPercent(BigDecimal percent) {
        BigDecimal safePercent = percent != null ? percent.setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        String sign = safePercent.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return sign + safePercent.stripTrailingZeros().toPlainString() + "%";
    }

    private Map<String, String> actionItem(String title, String description, String href) {
        Map<String, String> item = new HashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("href", href);
        return item;
    }

    private String ratingClass(PerformanceReview review) {
        if (review == null || review.getPerformanceScore() == null) {
            return "bg-slate-100 text-slate-700";
        }

        BigDecimal score = review.getPerformanceScore();
        if (score.compareTo(BigDecimal.valueOf(4)) >= 0) {
            return "bg-emerald-100 text-emerald-700";
        }
        if (score.compareTo(BigDecimal.valueOf(3)) >= 0) {
            return "bg-blue-100 text-blue-700";
        }
        if (score.compareTo(BigDecimal.valueOf(2)) >= 0) {
            return "bg-amber-100 text-amber-700";
        }
        return "bg-rose-100 text-rose-700";
    }

    private int countOpenPositions(List<Position> positions, Map<Long, Long> headcountByPositionId) {
        return (int) positions.stream()
                .filter(position -> headcountByPositionId.getOrDefault(position.getId(), 0L) == 0L)
                .count();
    }

    private BigDecimal calculateDepartmentPayroll(List<Employee> employees) {
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
        if (employeeIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<Long, Salary> latestSalaryByEmployeeId = new LinkedHashMap<>();
        for (Salary salary : salaryRepository.findLatestByEmployeeIds(employeeIds)) {
            latestSalaryByEmployeeId.putIfAbsent(salary.getEmployeeId(), salary);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Long employeeId : employeeIds) {
            Salary salary = latestSalaryByEmployeeId.get(employeeId);
            if (salary != null) {
                total = total.add(salary.getBaseAmount() != null ? salary.getBaseAmount() : BigDecimal.ZERO);
                total = total.add(salary.getAllowanceAmount() != null ? salary.getAllowanceAmount() : BigDecimal.ZERO);
            }
        }
        return total;
    }

    private String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount != null ? amount : BigDecimal.ZERO);
    }

    private RequestType findOrCreateRequestType(String code, String name, String category, String description) {
        return requestTypeRepository.findByCode(code).orElseGet(() -> {
            RequestType requestType = new RequestType();
            requestType.setCode(code);
            requestType.setName(name);
            requestType.setCategory(category);
            requestType.setDescription(description);
            return requestTypeRepository.save(requestType);
        });
    }

    private String buildRemovalRequestContent(Department department, Employee targetEmployee, String reason) {
        StringBuilder content = new StringBuilder();
        content.append("Department: ").append(department.getName()).append('\n');
        content.append("Target employee: ").append(displayEmployeeName(targetEmployee)).append('\n');
        content.append("Position: ").append(targetEmployee != null && targetEmployee.getPosition() != null
                ? targetEmployee.getPosition().getName()
                : "Unknown").append('\n');
        content.append("Reason: ").append(reason != null ? reason.trim() : "");
        return content.toString();
    }

    private String buildAddMemberRequestContent(Department department, String requestType, String role, String description) {
        StringBuilder content = new StringBuilder();
        content.append("Department: ").append(department.getName()).append('\n');
        content.append("Request type: ").append(requestType).append('\n');
        content.append("Position requested: ").append(role).append('\n');
        content.append("Description: ").append(description != null ? description.trim() : "");
        return content.toString();
    }

    private String displayEmployeeName(Employee employee) {
        if (employee == null) {
            return "Unknown employee";
        }
        if (employee.getUser() != null && employee.getUser().getFullName() != null) {
            return employee.getUser().getFullName();
        }
        return employee.getEmployeeCode() != null ? employee.getEmployeeCode() : "Employee #" + employee.getId();
    }

    private Map<String, String> mapStaffingUpdate(StaffingRequest request) {
        Map<String, String> item = new HashMap<>();

        String requestType = request.getRequestType() != null
                ? request.getRequestType().trim().toUpperCase(Locale.ROOT)
                : "REQUEST";
        String requestTypeLabel = "TRANSFER".equals(requestType) ? "Internal Transfer" : "Recruitment";
        String role = request.getRoleRequested() != null ? request.getRoleRequested() : "Role request";
        String status = request.getStatus() != null ? request.getStatus().trim().toUpperCase(Locale.ROOT) : "PENDING";

        item.put("requestType", requestTypeLabel);
        item.put("role", role);
        item.put("status", prettifyStatus(status));
        item.put("statusClass", staffingStatusClass(status));
        item.put("statusDotClass", staffingStatusDotClass(status));
        item.put("summary", staffingSummary(requestTypeLabel, role, status));
        item.put("detail", staffingDetail(request, status));
        item.put("timestamp", formatStaffingTimestamp(request));
        item.put("sortAt", resolveStaffingTimestamp(request).toString());
        item.put("sourceType", "STAFFING");
        item.put("requestId", String.valueOf(request.getId()));
        item.put("canCancel", String.valueOf("PENDING".equals(status)));
        return item;
    }

    private Map<String, String> mapRemovalUpdate(Request request) {
        Map<String, String> item = new HashMap<>();
        String status = request.getStatus() != null ? request.getStatus().trim().toUpperCase(Locale.ROOT) : "PENDING";
        String employeeName = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle().replaceFirst("^Removal request for\\s*", "")
                : displayEmployeeName(request.getEmployee());

        item.put("requestType", "Member Removal");
        item.put("role", employeeName);
        item.put("status", prettifyStatus(status));
        item.put("statusClass", staffingStatusClass(status));
        item.put("statusDotClass", staffingStatusDotClass(status));
        item.put("summary", removalSummary(employeeName, status));
        item.put("detail", removalDetail(request, status));
        item.put("timestamp", formatRemovalTimestamp(request));
        item.put("sortAt", resolveRemovalTimestamp(request).toString());
        item.put("sourceType", "REMOVAL");
        item.put("requestId", String.valueOf(request.getId()));
        item.put("canCancel", String.valueOf("PENDING".equals(status)));
        return item;
    }

    private String staffingSummary(String requestTypeLabel, String role, String status) {
        return switch (status) {
            case "COMPLETED" -> requestTypeLabel + " completed for " + role;
            case "APPROVED" -> requestTypeLabel + " approved for " + role;
            case "REJECTED" -> requestTypeLabel + " rejected for " + role;
            default -> requestTypeLabel + " submitted for " + role;
        };
    }

    private String staffingDetail(StaffingRequest request, String status) {
        if ("COMPLETED".equals(status) && request.getAssignedEmployee() != null) {
            return displayEmployeeName(request.getAssignedEmployee()) + " has already been assigned to your department.";
        }
        if ("APPROVED".equals(status)) {
            return "HR approved this staffing request and is finalizing the next action.";
        }
        if ("REJECTED".equals(status)) {
            return "HR closed this staffing request. Review with HR if you still need coverage.";
        }
        return "Your staffing request is currently waiting for HR review.";
    }

    private String removalSummary(String employeeName, String status) {
        return switch (status) {
            case "APPROVED" -> "Removal approved for " + employeeName;
            case "REJECTED" -> "Removal rejected for " + employeeName;
            default -> "Removal request submitted for " + employeeName;
        };
    }

    private String removalDetail(Request request, String status) {
        if ("APPROVED".equals(status)) {
            return "HR approved this department removal request. Coordinate the handoff and profile update with HR if needed.";
        }
        if ("REJECTED".equals(status)) {
            return request.getRejectedReason() != null && !request.getRejectedReason().isBlank()
                    ? "HR response: " + request.getRejectedReason()
                    : "HR rejected this department removal request.";
        }
        return "Your member removal request is still waiting for HR review.";
    }

    private String staffingStatusClass(String status) {
        return switch (status) {
            case "COMPLETED" -> "bg-emerald-100 text-emerald-700";
            case "APPROVED" -> "bg-blue-100 text-blue-700";
            case "REJECTED" -> "bg-rose-100 text-rose-700";
            default -> "bg-amber-100 text-amber-700";
        };
    }

    private String staffingStatusDotClass(String status) {
        return switch (status) {
            case "COMPLETED" -> "bg-emerald-500";
            case "APPROVED" -> "bg-blue-500";
            case "REJECTED" -> "bg-rose-500";
            default -> "bg-amber-500";
        };
    }

    private String prettifyStatus(String status) {
        return switch (status) {
            case "COMPLETED" -> "Completed";
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            case "CANCELLED" -> "Cancelled";
            default -> "Pending";
        };
    }

    private String formatStaffingTimestamp(StaffingRequest request) {
        LocalDateTime timestamp = resolveStaffingTimestamp(request);
        if (timestamp == null) {
            return "Just now";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
        return timestamp.format(formatter);
    }

    private LocalDateTime resolveStaffingTimestamp(StaffingRequest request) {
        return request.getProcessedAt() != null
                ? request.getProcessedAt()
                : (request.getUpdatedAt() != null ? request.getUpdatedAt() : request.getCreatedAt());
    }

    private String formatRemovalTimestamp(Request request) {
        LocalDateTime timestamp = resolveRemovalTimestamp(request);
        if (timestamp == null) {
            return "Just now";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
        return timestamp.format(formatter);
    }

    private LocalDateTime resolveRemovalTimestamp(Request request) {
        return request.getApprovedAt() != null
                ? request.getApprovedAt()
                : (request.getUpdatedAt() != null ? request.getUpdatedAt() : request.getCreatedAt());
    }

    private void saveHistory(Long requestId, Long approverId, String action, String comment) {
        if (requestId == null || approverId == null) {
            return;
        }
        RequestApprovalHistory history = new RequestApprovalHistory();
        history.setRequestId(requestId);
        history.setApproverId(approverId);
        history.setAction(action);
        history.setComment(comment);
        requestApprovalHistoryRepository.save(history);
    }
}
