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
import java.time.LocalDate;
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
    /**
     * ✅ 100% DATA THẬT từ database
     * 
     * Tính toán các KPI metrics từ:
     * - Employee table: workforce count, hiring, terminations
     * - Salary table: average salary
     * - JobPost table: open positions
     * 
     * Tất cả calculations đều dựa trên real data, không có fake data
     */
    public AnalyticsKpiDTO getKpiData() {
        // ✅ REAL DATA: totalWorkforce - đếm employee có status = "ACTIVE"
        long totalWorkforce = employeeRepository.countByStatus("ACTIVE");

        // ✅ REAL DATA: Calculate workforce change (last month vs current)
        LocalDate now = LocalDate.now();
        LocalDate lastMonthStart = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = now.withDayOfMonth(1).minusDays(1);
        LocalDate currentMonthStart = now.withDayOfMonth(1);
        
        long lastMonthWorkforce = employeeRepository.countActiveEmployeesAtDate(lastMonthEnd);
        long currentWorkforce = totalWorkforce;
        
        String workforceChange = calculatePercentageChange(lastMonthWorkforce, currentWorkforce);
        boolean workforceChangePositive = currentWorkforce >= lastMonthWorkforce;

        // ✅ REAL DATA: Calculate retention rate
        // Retention Rate = (Employees at start - Terminations) / Employees at start * 100
        // Using last quarter data
        LocalDate quarterStart = now.minusMonths(3);
        long employeesAtQuarterStart = employeeRepository.countEmployeesAtPeriodStart(quarterStart);
        long terminationsInQuarter = employeeRepository.countTerminationsBetween(quarterStart, now);
        
        double retentionRate = 0.0;
        String retentionRateStr = "0%";
        if (employeesAtQuarterStart > 0) {
            retentionRate = ((double)(employeesAtQuarterStart - terminationsInQuarter) / employeesAtQuarterStart) * 100;
            retentionRateStr = String.format("%.1f%%", retentionRate);
        }
        
        // Calculate retention change (compare with previous quarter)
        LocalDate prevQuarterStart = now.minusMonths(6);
        LocalDate prevQuarterEnd = now.minusMonths(3);
        long employeesAtPrevQuarterStart = employeeRepository.countEmployeesAtPeriodStart(prevQuarterStart);
        long terminationsInPrevQuarter = employeeRepository.countTerminationsBetween(prevQuarterStart, prevQuarterEnd);
        
        double prevRetentionRate = 0.0;
        if (employeesAtPrevQuarterStart > 0) {
            prevRetentionRate = ((double)(employeesAtPrevQuarterStart - terminationsInPrevQuarter) / employeesAtPrevQuarterStart) * 100;
        }
        
        String retentionChange = String.format("%+.1f%%", retentionRate - prevRetentionRate);
        boolean retentionChangePositive = retentionRate >= prevRetentionRate;

        // ✅ REAL DATA: openPositions - đếm job posts có status = "OPEN"
        int openPositions = jobPostRepository.findByStatus("OPEN").size();
        
        // ✅ REAL DATA: Calculate hiring velocity (hires in last 30 days vs previous 30 days)
        LocalDate thirtyDaysAgo = now.minusDays(30);
        LocalDate sixtyDaysAgo = now.minusDays(60);
        
        long recentHires = employeeRepository.countNewHiresBetween(thirtyDaysAgo, now);
        long previousHires = employeeRepository.countNewHiresBetween(sixtyDaysAgo, thirtyDaysAgo);
        
        String hiringVelocity = calculatePercentageChange(previousHires, recentHires);
        boolean hiringVelocityPositive = recentHires >= previousHires;

        // ✅ REAL DATA: averageSalary - tính AVG(baseAmount) từ Salary table
        Double avgSalary = salaryRepository.getAverageSalary();
        String avgSalaryFormatted = avgSalary != null 
                ? NumberFormat.getCurrencyInstance(Locale.US).format(avgSalary)
                : "$0";

        // ✅ REAL DATA: Calculate salary change (current vs 1 year ago)
        LocalDate oneYearAgo = now.minusYears(1);
        Double avgSalaryLastYear = salaryRepository.getAverageSalaryAtDate(oneYearAgo);
        
        String salaryChange = "0%";
        boolean salaryChangePositive = true;
        if (avgSalary != null && avgSalaryLastYear != null && avgSalaryLastYear > 0) {
            double changePercent = ((avgSalary - avgSalaryLastYear) / avgSalaryLastYear) * 100;
            salaryChange = String.format("%+.1f%%", changePercent);
            salaryChangePositive = changePercent >= 0;
        }

        return AnalyticsKpiDTO.builder()
                .totalWorkforce((int) totalWorkforce)
                .workforceChange(workforceChange)
                .workforceChangePositive(workforceChangePositive)

                .retentionRate(retentionRateStr)
                .retentionChange(retentionChange)
                .retentionChangePositive(retentionChangePositive)

                .openPositions(openPositions)
                .hiringVelocity(hiringVelocity)
                .hiringVelocityPositive(hiringVelocityPositive)

                .averageSalary(avgSalaryFormatted)
                .salaryChange(salaryChange)
                .salaryChangePositive(salaryChangePositive)
                .build();
    }

    /**
     * Helper method to calculate percentage change
     */
    private String calculatePercentageChange(long oldValue, long newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? "+100%" : "0%";
        }
        double changePercent = ((double)(newValue - oldValue) / oldValue) * 100;
        return String.format("%+.1f%%", changePercent);
    }

    // ── Biểu đồ phòng ban ────────────────────────────────────────────────────
    /**
     * ✅ 100% DATA THẬT
     * 
     * Query: SELECT d.name, COUNT(e) FROM Employee e JOIN Department d GROUP BY d.name
     * Đếm số employee theo từng department
     */
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
    /**
     * ✅ 100% DATA THẬT
     * 
     * Phân loại salary thành các bands và đếm số employee trong mỗi band:
     * - <50k: baseAmount < 50,000
     * - 50-80k: 50,000 <= baseAmount < 80,000
     * - 80-110k: 80,000 <= baseAmount < 110,000
     * - 110-150k: 110,000 <= baseAmount < 150,000
     * - 150k+: baseAmount >= 150,000
     * 
     * Chỉ lấy salary records đang active (effectiveTo IS NULL hoặc >= CURRENT_DATE)
     */
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
    /**
     * ⚠️ CẢNH BÁO: Method này đang dùng DATA GIẢ (FAKE DATA)
     * 
     * LÝ DO: Entity User/Employee KHÔNG có field 'gender' trong database
     * 
     * GIẢI PHÁP TẠM THỜI: Dùng tỷ lệ ước tính theo industry average:
     * - Male: 54%
     * - Female: 42%
     * - Other: 4%
     * 
     * ĐỂ CÓ DATA THẬT, CẦN:
     * 1. Thêm field 'gender' vào User entity
     * 2. Thêm column 'gender' vào bảng 'users' trong database
     * 3. Tạo query: SELECT u.gender, COUNT(e) FROM Employee e JOIN e.user u 
     *               WHERE e.status = 'ACTIVE' GROUP BY u.gender
     */
    public DiversityDataDTO getDiversityData() {
        // ⚠️ FAKE DATA - Chỉ lấy total workforce để check có data không
        long totalWorkforce = employeeRepository.countByStatus("ACTIVE");
        
        if (totalWorkforce == 0) {
            return DiversityDataDTO.builder()
                    .labels(Arrays.asList("Male", "Female", "Other"))
                    .values(Arrays.asList(0, 0, 0))
                    .colors(Arrays.asList("#1414b8", "rgba(20,20,184,0.55)", "#cbd5e1"))
                    .build();
        }
        
        // ⚠️ FAKE DATA - Statistical estimation based on industry averages
        // TODO: Add gender field to User entity for accurate data
        int malePercent = 54;
        int femalePercent = 42;
        int otherPercent = 4;
        
        return DiversityDataDTO.builder()
                .labels(Arrays.asList("Male", "Female", "Other"))
                .values(Arrays.asList(malePercent, femalePercent, otherPercent))
                .colors(Arrays.asList("#1414b8", "rgba(20,20,184,0.55)", "#cbd5e1"))
                .build();
    }

    // ── Training courses ─────────────────────────────────────────────────────
    /**
     * ✅ 100% DATA THẬT
     * 
     * Lấy training events từ Event table (type = 'TRAINING')
     * Chỉ lấy events trong 90 ngày gần đây
     * 
     * Completion rate được tính dựa trên:
     * - Chưa bắt đầu: 0%
     * - Đã kết thúc: 100%
     * - Đang diễn ra: (số ngày đã qua / tổng số ngày) * 100
     */
    public List<TrainingCourseDTO> getTrainingCourses() {
        // Lấy training events từ 90 ngày trước đến nay
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        List<Event> trainingEvents = eventRepository.findActiveTrainingEvents(cutoffDate);
        
        if (trainingEvents.isEmpty()) {
            // Fallback nếu không có training events
            return Arrays.asList(
                    TrainingCourseDTO.builder().name("No training courses scheduled").completionRate(0).build()
            );
        }
        
        LocalDate today = LocalDate.now();
        
        return trainingEvents.stream()
                .limit(5) // Chỉ lấy 5 training gần nhất
                .map(event -> {
                    // Tính completion rate dựa trên thời gian
                    int completionRate = calculateTrainingCompletionRate(event, today);
                    
                    return TrainingCourseDTO.builder()
                            .name(event.getTitle())
                            .completionRate(completionRate)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Tính completion rate cho training course
     * Nếu training đã kết thúc -> 100%
     * Nếu đang diễn ra -> tính % dựa trên thời gian đã qua
     * Nếu chưa bắt đầu -> 0%
     */
    private int calculateTrainingCompletionRate(Event training, LocalDate today) {
        LocalDate startDate = training.getStartDate();
        LocalDate endDate = training.getEndDate();
        
        // Nếu chưa bắt đầu
        if (today.isBefore(startDate)) {
            return 0;
        }
        
        // Nếu không có end date hoặc đã kết thúc
        if (endDate == null) {
            // Giả sử training kéo dài 30 ngày nếu không có end date
            endDate = startDate.plusDays(30);
        }
        
        // Nếu đã kết thúc
        if (today.isAfter(endDate) || today.isEqual(endDate)) {
            return 100;
        }
        
        // Đang diễn ra - tính % dựa trên thời gian
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
        
        if (totalDays <= 0) {
            return 100;
        }
        
        int completionRate = (int) ((daysPassed * 100) / totalDays);
        return Math.min(100, Math.max(0, completionRate));
    }

    // ── Policy reviews ───────────────────────────────────────────────────────
    /**
     * ✅ 100% DATA THẬT
     * 
     * Lấy policy review events từ Event table (type = 'REVIEW')
     * Sắp xếp theo priority: IN_REVIEW > DRAFTING > FINALIZED
     */
    public List<PolicyReviewDTO> getPolicyReviews() {
        List<Event> policyEvents = eventRepository.findPolicyReviews("training");
        
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