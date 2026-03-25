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
import com.group5.ems.entity.User;
import com.group5.ems.repository.AttendanceRepository;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.PerformanceReviewRepository;
import com.group5.ems.repository.PositionRepository;
import com.group5.ems.repository.RequestApprovalHistoryRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.RequestTypeRepository;
import com.group5.ems.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeptManagerService {

    private static final String DEFAULT_AVATAR =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAx3bm_6ROku45Qad2UC6L8WqGYQTSxbQfGbrIsZyy-UW0G-0eeaUe05OzGGUPVXtUgSAXYY1km4lsQ8OMlKocQqnLvoWylgqv8HhjdOhc-kA7_Y9WGXOHncHiVIom2GDXi5UFfTRWNw-kIM5Tj5rLVJx3alhzAv1liLktNE8Zt65-kYJuInGPkWm85aD_STgeoCKnakLN1ZpxNfG-GLOhHh26_zxMgT8NQ21STEfw2DrFNb7ygWY6IQKmzRFuP-NmzVNfiEHO9zvA";

    private final DepartmentRepository departmentRepository;
    private final DeptManagerUtilService utilService;
    private final RequestRepository requestRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final PositionRepository positionRepository;
    private final AttendanceRepository attendanceRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final SalaryRepository salaryRepository;
    private final RequestApprovalHistoryRepository requestApprovalHistoryRepository;

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
        Map<Long, String> weeklyAttendanceByEmployee = getWeeklyAttendanceByEmployee(employees);

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
        data.put("recentTeamActivities", buildRecentTeamActivities(employees, latestReviewByEmployee, weeklyAttendanceByEmployee));
        data.put("actionItems", buildDashboardActionItems(department, employees, latestReviewByEmployee));
        data.put("statusBreakdown", buildStatusBreakdown(activeCount, inactiveCount, suspendedCount));

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
            return data;
        }

        List<Employee> employees = safeEmployees(department);
        List<Position> departmentPositions = positionRepository.findByDepartmentId(department.getId());

        departmentMap.put("name", department.getName() != null ? department.getName() : "Unnamed Department");
        departmentMap.put("code", department.getCode() != null ? department.getCode() : "N/A");
        departmentMap.put("description", department.getDescription() != null
                ? department.getDescription()
                : "Department operations run here.");
        departmentMap.put("manager", managerMap.get("name"));
        departmentMap.put("totalEmployees", String.valueOf(employees.size()));
        departmentMap.put("openPositions", String.valueOf(countOpenPositions(departmentPositions)));
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
            long headcount = employees.stream()
                    .filter(employee -> position.getId().equals(employee.getPositionId()))
                    .count();

            Map<String, String> positionMap = new HashMap<>();
            positionMap.put("title", position.getName());
            positionMap.put("headcount", String.valueOf(headcount));
            positionMap.put("status", headcount > 0 ? "Filled" : "Open");
            positionMap.put("statusClass", headcount > 0
                    ? "bg-green-100 text-green-700"
                    : "bg-amber-100 text-amber-700");
            positions.add(positionMap);
        }

        data.put("department", departmentMap);
        data.put("teams", teams);
        data.put("positions", positions);

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
        RequestType workflowType = "TRANSFER".equals(normalizedType)
                ? findOrCreateRequestType("HR_TRANSFER", "Internal Transfer Request", "HR_STATUS",
                "Request to transfer an employee into the department")
                : findOrCreateRequestType("HR_RECRUIT", "Recruitment Request", "HR_STATUS",
                "Propose hiring new staff for the department");

        Request request = new Request();
        request.setEmployeeId(managerEmployee.getId());
        request.setRequestTypeId(workflowType.getId());
        request.setTitle(("TRANSFER".equals(normalizedType) ? "Transfer request: " : "Recruitment request: ") + role);
        request.setContent(buildAddMemberRequestContent(department, normalizedType, role, description));
        request.setStatus("PENDING");
        Request savedRequest = requestRepository.save(request);
        saveHistory(savedRequest.getId(), managerEmployee.getUserId(), "SUBMITTED", "Submitted by Department Manager");
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

    private Map<Long, String> getWeeklyAttendanceByEmployee(List<Employee> employees) {
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        Map<Long, List<Attendance>> grouped = attendanceRepository
                .findByEmployeeIdInAndWorkDateBetweenOrderByWorkDateAsc(employeeIds, monday, sunday)
                .stream()
                .collect(Collectors.groupingBy(Attendance::getEmployeeId));

        Map<Long, String> attendanceSummary = new HashMap<>();
        for (Employee employee : employees) {
            List<Attendance> attendances = grouped.getOrDefault(employee.getId(), List.of());
            long present = attendances.stream().filter(att -> "PRESENT".equalsIgnoreCase(att.getStatus())).count();
            long late = attendances.stream().filter(att -> "LATE".equalsIgnoreCase(att.getStatus())).count();
            long absent = attendances.stream().filter(att -> "ABSENT".equalsIgnoreCase(att.getStatus())).count();

            if (absent > 0) {
                attendanceSummary.put(employee.getId(), "Absence (" + absent + ")");
            } else if (late > 0) {
                attendanceSummary.put(employee.getId(), "Late (" + late + ")");
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
        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdInAndWorkDateBetweenOrderByWorkDateAsc(employeeIds, monday, sunday);

        long accountableRecords = attendances.stream()
                .filter(att -> "PRESENT".equalsIgnoreCase(att.getStatus())
                        || "LATE".equalsIgnoreCase(att.getStatus())
                        || "ABSENT".equalsIgnoreCase(att.getStatus()))
                .count();
        long attended = attendances.stream()
                .filter(att -> "PRESENT".equalsIgnoreCase(att.getStatus())
                        || "LATE".equalsIgnoreCase(att.getStatus()))
                .count();

        if (accountableRecords == 0) {
            return "0%";
        }

        long percentage = Math.round((double) attended * 100 / accountableRecords);
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
                                                                Map<Long, String> weeklyAttendanceByEmployee) {
        return employees.stream()
                .sorted(Comparator.comparing(this::latestTouchPoint).reversed())
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

    private LocalDateTime latestTouchPoint(Employee employee) {
        Optional<PerformanceReview> review = performanceReviewRepository.findByEmployeeId(employee.getId()).stream()
                .max(Comparator.comparing(item -> item.getUpdatedAt() != null ? item.getUpdatedAt() : item.getCreatedAt()));
        return review.map(item -> item.getUpdatedAt() != null ? item.getUpdatedAt() : item.getCreatedAt())
                .orElse(employee.getUpdatedAt() != null ? employee.getUpdatedAt() : employee.getCreatedAt());
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
        items.add(statusItem("Active", String.valueOf(activeCount), "bg-green-500"));
        items.add(statusItem("On Leave / Inactive", String.valueOf(inactiveCount), "bg-amber-500"));
        items.add(statusItem("Other", String.valueOf(suspendedCount), "bg-slate-500"));
        return items;
    }

    private Map<String, String> statusItem(String label, String value, String colorClass) {
        Map<String, String> item = new HashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("colorClass", colorClass);
        return item;
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

    private int countOpenPositions(List<Position> positions) {
        return (int) positions.stream().filter(position -> position.getEmployees() == null || position.getEmployees().isEmpty()).count();
    }

    private BigDecimal calculateDepartmentPayroll(List<Employee> employees) {
        BigDecimal total = BigDecimal.ZERO;
        for (Employee employee : employees) {
            Salary salary = salaryRepository.findTopByEmployeeIdOrderByEffectiveFromDesc(employee.getId()).orElse(null);
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
