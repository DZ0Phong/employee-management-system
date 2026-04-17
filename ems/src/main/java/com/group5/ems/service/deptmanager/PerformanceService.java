package com.group5.ems.service.deptmanager;

import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.PerformanceReview;
import com.group5.ems.entity.User;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.EmployeeSkillRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.PerformanceReviewRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final DeptManagerUtilService utilService;
    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final LogService logService;

    public Map<String, Object> getPerformanceReviewData() {
        Map<String, Object> data = new HashMap<>();

        User currentUser = utilService.getCurrentUser();
        Department department = utilService.getDepartmentForManager(currentUser);
        Employee managerEmployee = utilService.getCurrentEmployee();

        data.put("manager", utilService.getManagerMap(currentUser));
        data.put("pendingApprovals", utilService.getPendingApprovalsCount(department));

        if (department == null) {
            data.put("reviews", List.of());
            data.put("employees", List.of());
            data.put("reviewPeriodOptions", buildReviewPeriodOptions());
            data.put("notStartedCount", 0);
            data.put("overdueCount", 0);
            data.put("draftCount", 0);
            data.put("scheduledCount", 0);
            data.put("completedCount", 0);
            data.put("completionRate", "0%");
            data.put("averageGrowth", "0%");
            return data;
        }

        List<Employee> employees = employeeRepository.findByDepartmentIdWithUser(department.getId()).stream()
                .filter(this::isReviewableEmployee)
                .filter(employee -> managerEmployee == null || !Objects.equals(employee.getId(), managerEmployee.getId()))
                .sorted(Comparator.comparing(
                        (Employee employee) -> employee.getUser() != null ? employee.getUser().getFullName() : "",
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
        List<PerformanceReview> departmentReviews = reviewRepository.findByEmployee_DepartmentIdOrderByUpdatedAtDesc(department.getId());

        Map<Long, PerformanceReview> latestReviewByEmployee = new LinkedHashMap<>();
        Map<Long, List<PerformanceReview>> reviewsByEmployee = new HashMap<>();
        for (PerformanceReview review : departmentReviews) {
            latestReviewByEmployee.putIfAbsent(review.getEmployeeId(), review);
            reviewsByEmployee.computeIfAbsent(review.getEmployeeId(), key -> new ArrayList<>()).add(review);
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        int overdueCount = 0;
        int notStartedCount = 0;
        int draftCount = 0;
        int scheduledCount = 0;
        int completedCount = 0;

        List<Map<String, Object>> reviews = new ArrayList<>();
        for (Employee employee : employees) {
            PerformanceReview review = latestReviewByEmployee.get(employee.getId());
            if (review == null) {
                Map<String, Object> row = buildReviewRowFromEmployee(employee);
                row.put("status", "NOT_STARTED");
                row.put("statusDisplay", "Not Started");
                row.put("statusTheme", "bg-slate-100 text-slate-700 border-slate-200");
                row.put("actionLabel", "Start Review");
                row.put("actionMode", "create");
                notStartedCount++;
                reviews.add(row);
                continue;
            }

            String computedStatus = computeDisplayStatus(review);
            if ("OVERDUE".equals(computedStatus)) {
                overdueCount++;
            } else if ("DRAFT".equals(computedStatus)) {
                draftCount++;
            } else if ("SCHEDULED".equals(computedStatus)) {
                scheduledCount++;
            } else if ("COMPLETED".equals(computedStatus)) {
                completedCount++;
            }

            Map<String, Object> row = buildReviewRowFromEmployee(employee);
            row.put("id", review.getId());
            row.put("cyclePeriod", review.getReviewPeriod());
            row.put("reviewDate", review.getUpdatedAt() != null ? review.getUpdatedAt().format(dateFormatter) : "N/A");
            row.put("status", computedStatus);
            row.put("statusDisplay", prettifyStatus(computedStatus));
            row.put("statusTheme", statusTheme(computedStatus));
            row.put("actionLabel", actionLabel(computedStatus));
            row.put("actionMode", "edit");
            row.put("performanceScore", review.getPerformanceScore());
            row.put("potentialScore", review.getPotentialScore());
            row.put("strengths", review.getStrengths());
            row.put("areasToImprove", review.getAreasToImprove());
            row.put("reviewerName", review.getReviewer() != null && review.getReviewer().getUser() != null
                    ? review.getReviewer().getUser().getFullName()
                    : "N/A");
            reviews.add(row);
        }

        reviews.sort(Comparator
                .comparingInt((Map<String, Object> row) -> reviewStatusPriority((String) row.get("status")))
                .thenComparing(row -> ((String) row.getOrDefault("employeeName", "")).toLowerCase(Locale.ROOT)));

        int completionRate = !employees.isEmpty()
                ? (int) Math.round((double) completedCount * 100 / employees.size())
                : 0;

        List<Map<String, Object>> employeeOptions = employees.stream()
                .map(employee -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", employee.getId());
                    item.put("name", employee.getUser() != null ? employee.getUser().getFullName() : "Employee #" + employee.getId());
                    item.put("position", employee.getPosition() != null ? employee.getPosition().getName() : "Employee");
                    item.put("skillsLabel", buildSkillsLabel(employee.getId()));
                    return item;
                })
                .collect(Collectors.toList());

        data.put("reviews", reviews);
        data.put("employees", employeeOptions);
        data.put("reviewPeriodOptions", buildReviewPeriodOptions());
        data.put("notStartedCount", notStartedCount);
        data.put("overdueCount", overdueCount);
        data.put("draftCount", draftCount);
        data.put("scheduledCount", scheduledCount);
        data.put("completedCount", completedCount);
        data.put("completionRate", completionRate + "%");
        data.put("averageGrowth", calculateAverageGrowth(reviewsByEmployee));

        return data;
    }

    @Transactional
    public Long savePerformanceReview(Long reviewId,
                                      Long employeeId,
                                      String reviewPeriod,
                                      BigDecimal performanceScore,
                                      BigDecimal potentialScore,
                                      String strengths,
                                      String areasToImprove,
                                      String status) {
        Department department = utilService.requireCurrentManagedDepartment();
        Employee managerEmployee = utilService.requireCurrentEmployee();
        String normalizedStatus = normalizeStatus(status);
        BigDecimal safePerformanceScore = normalizeScore(performanceScore);
        BigDecimal safePotentialScore = normalizeScore(potentialScore);

        PerformanceReview review;
        Employee targetEmployee;
        String normalizedPeriod;
        if (reviewId != null) {
            review = reviewRepository.findByIdAndEmployee_DepartmentId(reviewId, department.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Performance review not found."));
            targetEmployee = review.getEmployee() != null
                    ? review.getEmployee()
                    : employeeRepository.findById(review.getEmployeeId())
                            .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
            normalizedPeriod = review.getReviewPeriod();
        } else {
            targetEmployee = employeeRepository.findById(employeeId)
                    .filter(employee -> department.getId().equals(employee.getDepartmentId()))
                    .orElseThrow(() -> new IllegalArgumentException("Employee is outside your department."));
            normalizedPeriod = normalizeReviewPeriod(reviewPeriod);
            if (reviewRepository.findByEmployeeIdAndReviewPeriod(targetEmployee.getId(), normalizedPeriod).isPresent()) {
                throw new IllegalArgumentException("A performance review already exists for this employee and review period.");
            }
            review = new PerformanceReview();
        }

        if (!isReviewableEmployee(targetEmployee)) {
            throw new IllegalArgumentException("Only active or on-leave employees can be reviewed.");
        }
        if (Objects.equals(targetEmployee.getId(), managerEmployee.getId())) {
            throw new IllegalArgumentException("Department managers cannot submit performance reviews for themselves.");
        }
        if ("COMPLETED".equals(normalizedStatus)) {
            if (trimToNull(strengths) == null || trimToNull(areasToImprove) == null) {
                throw new IllegalArgumentException("Completed reviews must include both strengths and areas to improve.");
            }
        }

        review.setEmployeeId(targetEmployee.getId());
        review.setReviewerId(managerEmployee.getId());
        review.setReviewPeriod(normalizedPeriod);
        review.setPerformanceScore(safePerformanceScore);
        review.setPotentialScore(safePotentialScore);
        review.setStrengths(trimToNull(strengths));
        review.setAreasToImprove(trimToNull(areasToImprove));
        review.setStatus(normalizedStatus);

        boolean creating = review.getId() == null;
        PerformanceReview savedReview = reviewRepository.save(review);
        logService.log(creating ? AuditAction.CREATE : AuditAction.UPDATE, AuditEntityType.PERFORMANCE, savedReview.getId());
        return savedReview.getId();
    }

    public Map<String, Object> getReviewDetail(Long id) {
        Department department = utilService.requireCurrentManagedDepartment();
        PerformanceReview review = reviewRepository.findByIdAndEmployee_DepartmentId(id, department.getId())
                .orElseThrow(() -> new IllegalArgumentException("Performance review not found."));

        Employee employee = review.getEmployee();
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", review.getId());
        detail.put("employeeId", review.getEmployeeId());
        detail.put("employeeName", employee != null && employee.getUser() != null
                ? employee.getUser().getFullName()
                : "Unknown");
        detail.put("employeeTitle", employee != null && employee.getPosition() != null
                ? employee.getPosition().getName()
                : "Employee");
        detail.put("avatarUrl", employee != null && employee.getUser() != null ? employee.getUser().getAvatarUrl() : null);
        detail.put("reviewPeriod", review.getReviewPeriod());
        detail.put("performanceScore", review.getPerformanceScore());
        detail.put("potentialScore", review.getPotentialScore());
        detail.put("strengths", review.getStrengths());
        detail.put("areasToImprove", review.getAreasToImprove());
        detail.put("status", normalizeStatus(review.getStatus()));
        detail.put("statusDisplay", prettifyStatus(computeDisplayStatus(review)));
        detail.put("reviewDate", review.getUpdatedAt() != null
                ? review.getUpdatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                : "N/A");
        detail.put("reviewerName", review.getReviewer() != null && review.getReviewer().getUser() != null
                ? review.getReviewer().getUser().getFullName()
                : "N/A");
        detail.put("skillsLabel", buildSkillsLabel(review.getEmployeeId()));
        return detail;
    }

    private Map<String, Object> buildReviewRowFromEmployee(Employee employee) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", null);
        row.put("employeeId", employee.getId());
        row.put("employeeName", employee.getUser() != null ? employee.getUser().getFullName() : "Employee #" + employee.getId());
        row.put("employeeTitle", employee.getPosition() != null ? employee.getPosition().getName() : "Employee");
        row.put("avatarUrl", employee.getUser() != null ? employee.getUser().getAvatarUrl() : null);
        row.put("cyclePeriod", "Not set");
        row.put("reviewDate", "N/A");
        row.put("status", "NOT_STARTED");
        row.put("statusDisplay", "Not Started");
        row.put("statusTheme", "bg-slate-100 text-slate-700 border-slate-200");
        row.put("actionLabel", "Start Review");
        row.put("actionMode", "create");
        row.put("performanceScore", null);
        row.put("potentialScore", null);
        row.put("strengths", null);
        row.put("areasToImprove", null);
        row.put("reviewerName", "Department Manager");
        return row;
    }

    private String buildSkillsLabel(Long employeeId) {
        List<String> skills = employeeSkillRepository.findByEmployeeId(employeeId).stream()
                .filter(employeeSkill -> employeeSkill.getSkill() != null && employeeSkill.getSkill().getName() != null)
                .sorted(Comparator.comparing(employeeSkill -> employeeSkill.getSkill().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(employeeSkill -> {
                    String skillName = employeeSkill.getSkill().getName();
                    Integer proficiency = employeeSkill.getProficiency();
                    return proficiency != null ? skillName + " (" + proficiency + "/5)" : skillName;
                })
                .toList();
        if (skills.isEmpty()) {
            return "No verified skills recorded yet";
        }
        return String.join(" • ", skills);
    }

    private String computeDisplayStatus(PerformanceReview review) {
        String normalized = normalizeStatus(review.getStatus());
        if ("COMPLETED".equals(normalized)) {
            return normalized;
        }

        LocalDate dueDate = extractDueDate(review.getReviewPeriod());
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return "OVERDUE";
        }
        return normalized;
    }

    private String statusTheme(String status) {
        return switch (status) {
            case "COMPLETED" -> "bg-emerald-50 text-emerald-600 border-emerald-100";
            case "SCHEDULED" -> "bg-blue-50 text-blue-600 border-blue-100";
            case "OVERDUE" -> "bg-rose-50 text-rose-600 border-rose-100";
            case "DRAFT" -> "bg-amber-50 text-amber-600 border-amber-100";
            default -> "bg-slate-100 text-slate-700 border-slate-200";
        };
    }

    private String actionLabel(String status) {
        return switch (status) {
            case "COMPLETED" -> "View Summary";
            case "SCHEDULED" -> "Prepare Review";
            case "OVERDUE", "DRAFT" -> "Continue Review";
            default -> "Start Review";
        };
    }

    private String prettifyStatus(String status) {
        return switch (status) {
            case "NOT_STARTED" -> "Not Started";
            case "COMPLETED" -> "Completed";
            case "SCHEDULED" -> "Scheduled";
            case "OVERDUE" -> "Overdue";
            case "DRAFT" -> "Draft";
            default -> status;
        };
    }

    private String calculateAverageGrowth(Map<Long, List<PerformanceReview>> byEmployee) {
        List<BigDecimal> deltas = new ArrayList<>();
        for (List<PerformanceReview> reviews : byEmployee.values()) {
            List<PerformanceReview> sorted = reviews.stream()
                    .filter(review -> review.getPerformanceScore() != null)
                    .sorted(Comparator.comparing((PerformanceReview review) ->
                            review.getUpdatedAt() != null ? review.getUpdatedAt() : review.getCreatedAt()).reversed())
                    .toList();

            if (sorted.size() < 2) {
                continue;
            }

            BigDecimal latest = sorted.get(0).getPerformanceScore();
            BigDecimal previous = sorted.get(1).getPerformanceScore();
            if (latest == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal deltaPercent = latest.subtract(previous)
                    .divide(previous, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            deltas.add(deltaPercent);
        }

        if (deltas.isEmpty()) {
            return "0%";
        }

        BigDecimal total = deltas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = total.divide(BigDecimal.valueOf(deltas.size()), 1, RoundingMode.HALF_UP);
        String sign = average.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return sign + average.stripTrailingZeros().toPlainString() + "%";
    }

    private String normalizeReviewPeriod(String rawReviewPeriod) {
        if (rawReviewPeriod == null || rawReviewPeriod.isBlank()) {
            throw new IllegalArgumentException("Please choose a review period.");
        }
        String normalized = rawReviewPeriod.trim().toUpperCase(Locale.ROOT);
        if (normalized.matches("YEAR_\\d{4}") || normalized.matches("H[12]_\\d{4}")) {
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported review period.");
    }

    private String normalizeStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "DRAFT";
        }

        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DRAFT", "SCHEDULED", "COMPLETED" -> normalized;
            default -> "DRAFT";
        };
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.valueOf(3);
        }
        BigDecimal normalized = score.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ONE) < 0) {
            return BigDecimal.ONE;
        }
        if (normalized.compareTo(BigDecimal.valueOf(5)) > 0) {
            return BigDecimal.valueOf(5).setScale(2, RoundingMode.HALF_UP);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private boolean isReviewableEmployee(Employee employee) {
        if (employee == null) {
            return false;
        }
        String status = employee.getStatus() == null ? "" : employee.getStatus().trim().toUpperCase(Locale.ROOT);
        return "ACTIVE".equals(status) || "ON_LEAVE".equals(status);
    }

    private int reviewStatusPriority(String status) {
        return switch (status == null ? "" : status) {
            case "OVERDUE" -> 0;
            case "DRAFT" -> 1;
            case "SCHEDULED" -> 2;
            case "NOT_STARTED" -> 3;
            case "COMPLETED" -> 4;
            default -> 5;
        };
    }

    private List<String> buildReviewPeriodOptions() {
        int currentYear = LocalDate.now().getYear();
        List<String> options = new ArrayList<>();
        for (int year = currentYear - 1; year <= currentYear + 1; year++) {
            options.add("YEAR_" + year);
            options.add("H1_" + year);
            options.add("H2_" + year);
        }
        return options;
    }

    private LocalDate extractDueDate(String reviewPeriod) {
        if (reviewPeriod == null || reviewPeriod.isBlank()) {
            return null;
        }

        String normalized = reviewPeriod.trim().toUpperCase(Locale.ROOT);
        try {
            if (normalized.startsWith("YEAR_")) {
                int year = Integer.parseInt(normalized.substring(5));
                return LocalDate.of(year, Month.DECEMBER, 31);
            }
            if (normalized.startsWith("H1_")) {
                int year = Integer.parseInt(normalized.substring(3));
                return LocalDate.of(year, Month.JUNE, 30);
            }
            if (normalized.startsWith("H2_")) {
                int year = Integer.parseInt(normalized.substring(3));
                return LocalDate.of(year, Month.DECEMBER, 31);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }
}
