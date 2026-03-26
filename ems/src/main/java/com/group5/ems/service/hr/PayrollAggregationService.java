package com.group5.ems.service.hr;

import com.group5.ems.dto.response.hr.EmployeeAggregationDTO;
import com.group5.ems.dto.response.hr.PeriodSummaryDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Payslip;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.Salary;
import com.group5.ems.entity.TimesheetPeriod;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.exception.PayrollPreviewNotFoundException;
import com.group5.ems.repository.BonusRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.PayslipRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.RewardDisciplineRepository;
import com.group5.ems.repository.SalaryRepository;
import com.group5.ems.repository.TimesheetPeriodRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for aggregating payroll data (attendance, bonuses, deductions)
 * for a given timesheet period. Used by the Payroll Preview dashboard.
 */
@Service
@RequiredArgsConstructor
public class PayrollAggregationService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TimesheetPeriodRepository periodRepository;
    private final EmployeeRepository employeeRepository;
    private final RequestRepository requestRepository;
    private final BonusRepository bonusRepository;
    private final RewardDisciplineRepository rewardDisciplineRepository;
    private final SalaryRepository salaryRepository;
    private final PayslipRepository payslipRepository;
    private final LogService logService;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Aggregates payroll data for all active employees in a given period.
     *
     * @param periodId the timesheet period ID
     * @return list of per-employee aggregation DTOs
     * @throws PayrollPreviewNotFoundException if period does not exist
     */
    @Transactional(readOnly = true)
    public List<EmployeeAggregationDTO> previewAllEmployees(Long periodId) {
        TimesheetPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new PayrollPreviewNotFoundException(periodId));

        List<Employee> eligibleEmployees = employeeRepository.findEligibleEmployeesForPeriodList(period.getEndDate());
        List<EmployeeAggregationDTO> result = new ArrayList<>();

        for (Employee emp : eligibleEmployees) {
            result.add(aggregateDataForEmployee(period, emp));
        }

        return result;
    }

    /**
     * Returns a truly paginated slice of per-employee aggregation DTOs.
     * Queries only the requested page of active employees from the database,
     * then aggregates payroll data for those employees only.
     *
     * @param periodId the timesheet period ID
     * @param pageable pagination parameters
     * @return paginated employee aggregation data
     * @throws PayrollPreviewNotFoundException if period does not exist
     */
    @Transactional(readOnly = true)
    public Page<EmployeeAggregationDTO> getPaginatedPreview(Long periodId, Pageable pageable) {
        TimesheetPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new PayrollPreviewNotFoundException(periodId));

        // Query only the requested page of employees from DB
        Page<Employee> employeePage = employeeRepository.findEligibleEmployeesForPeriod(period.getEndDate(), pageable);

        List<EmployeeAggregationDTO> aggregated = employeePage.getContent().stream()
                .map(emp -> aggregateDataForEmployee(period, emp))
                .toList();

        return new PageImpl<>(aggregated, pageable, employeePage.getTotalElements());
    }

    /**
     * Calculates top-level summary metrics for the preview dashboard cards.
     * Uses the Page's totalElements for employee count (from DB count query)
     * and computes avgPayableDays from the current page content.
     *
     * @param periodId the timesheet period ID
     * @param page     paginated employee aggregation data
     * @return period summary DTO for dashboard cards
     * @throws PayrollPreviewNotFoundException if period does not exist
     */
    @Transactional(readOnly = true)
    public PeriodSummaryDTO getPeriodSummary(Long periodId, Page<EmployeeAggregationDTO> page) {
        TimesheetPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new PayrollPreviewNotFoundException(periodId));

        double avgPayableDays = page.getContent().stream()
                .mapToDouble(EmployeeAggregationDTO::payableDays)
                .average()
                .orElse(0.0);

        return PeriodSummaryDTO.builder()
                .periodId(period.getId())
                .periodName(period.getPeriodName())
                .locked(Boolean.TRUE.equals(period.getIsLocked()))
                .startDate(period.getStartDate())
                .endDate(period.getEndDate())
                .totalEmployeesProcessed((int) page.getTotalElements())
                .avgPayableDays(Math.round(avgPayableDays * 100.0) / 100.0)
                .build();
    }

    /**
     * Generates payslips for all active employees in a locked period.
     * Uses aggregated data + base salary to compute net salary.
     *
     * @param periodId the timesheet period ID (must be locked)
     * @return the number of payslips generated
     * @throws PayrollPreviewNotFoundException if period does not exist
     * @throws IllegalStateException           if period is not locked or payslips already exist
     */
    @Transactional
    public int generatePayslips(Long periodId) {
        TimesheetPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new PayrollPreviewNotFoundException(periodId));

        if (!Boolean.TRUE.equals(period.getIsLocked())) {
            throw new IllegalStateException("Cannot generate payslips for an unlocked period. Please lock the period first.");
        }

        // Check if payslips already exist for this period
        List<Payslip> existingPayslips = payslipRepository.findByPeriodId(periodId);
        if (!existingPayslips.isEmpty()) {
            throw new IllegalStateException("Payslips have already been generated for this period. Found "
                    + existingPayslips.size() + " existing payslip(s).");
        }

        List<Employee> eligibleEmployees = employeeRepository.findEligibleEmployeesForPeriodList(period.getEndDate());
        List<Payslip> payslips = new ArrayList<>();

        for (Employee emp : eligibleEmployees) {
            EmployeeAggregationDTO agg = aggregateDataForEmployee(period, emp);

            // Look up the employee's current base salary
            BigDecimal baseSalary = salaryRepository
                    .findFirstByEmployeeIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                            emp.getId(), period.getEndDate())
                    .map(Salary::getBaseAmount)
                    .orElse(BigDecimal.ZERO);

            // Calculate prorated base: (baseSalary / standardDays) * payableDays
            BigDecimal actualBase = BigDecimal.ZERO;
            if (agg.standardDays() > 0) {
                actualBase = baseSalary
                        .multiply(BigDecimal.valueOf(agg.payableDays()))
                        .divide(BigDecimal.valueOf(agg.standardDays()), 2, RoundingMode.HALF_UP);
            }

            // OT amount: baseSalary / standardDays / 8 * 1.5 * totalOvertimeHours
            BigDecimal otAmount = BigDecimal.ZERO;
            if (agg.standardDays() > 0 && agg.totalOvertimeHours().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal hourlyRate = baseSalary
                        .divide(BigDecimal.valueOf(agg.standardDays()), 4, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);
                otAmount = hourlyRate
                        .multiply(BigDecimal.valueOf(1.5))
                        .multiply(agg.totalOvertimeHours())
                        .setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal netSalary = actualBase
                    .add(otAmount)
                    .add(agg.totalBonuses())
                    .subtract(agg.totalDeductions());

            Payslip payslip = new Payslip();
            payslip.setEmployeeId(emp.getId());
            payslip.setPeriodId(periodId);
            payslip.setActualBaseSalary(actualBase);
            payslip.setTotalOtAmount(otAmount);
            payslip.setTotalBonus(agg.totalBonuses());
            payslip.setTotalDeduction(agg.totalDeductions());
            payslip.setNetSalary(netSalary);
            payslip.setStatus("PENDING");

            payslips.add(payslip);
        }

        payslipRepository.saveAll(payslips);

        // Rule #15: Log generation action
        logService.log(AuditAction.CREATE, AuditEntityType.PAYROLL_PREVIEW, periodId);

        return payslips.size();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /**
     * Aggregates all payroll-relevant data for a single employee within a period.
     * Uses Request.startDate / Request.endDate (Instant) for leave and OT queries.
     */
    private EmployeeAggregationDTO aggregateDataForEmployee(TimesheetPeriod period, Employee emp) {
        LocalDate start = period.getStartDate();
        LocalDate end = period.getEndDate();
        Long empId = emp.getId();

        // Convert period dates to Instant (Vietnam timezone) for Request queries
        Instant periodStartInstant = toStartOfDayInstant(start);
        Instant periodEndInstant = toEndOfDayInstant(end);

        // 1. Standard weekdays (Mon-Fri)
        int standardDays = countWeekdays(start, end);

        // 2. Unpaid leave days (clipped to period range)
        double unpaidLeaveDays = 0.0;
        List<Request> unpaidLeaves = requestRepository.findApprovedUnpaidLeave(empId, periodStartInstant, periodEndInstant);
        for (Request leave : unpaidLeaves) {
            unpaidLeaveDays += calculateOverlappingWeekdays(leave.getStartDate(), leave.getEndDate(), start, end);
        }

        // 3. Payable days
        double payableDays = Math.max(0, standardDays - unpaidLeaveDays);

        // 4. Overtime hours = number of overlapping weekdays from OT requests × 8h per day
        BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        List<Request> overtimeRequests = requestRepository.findApprovedOvertime(empId, periodStartInstant, periodEndInstant);
        for (Request ot : overtimeRequests) {
            long otDays = calculateOverlappingWeekdays(ot.getStartDate(), ot.getEndDate(), start, end);
            totalOvertimeHours = totalOvertimeHours.add(BigDecimal.valueOf(otDays * 8L));
        }

        // 5. Bonuses
        BigDecimal totalBonuses = bonusRepository.sumApprovedBonuses(empId, start, end);

        // 6. Deductions (discipline fines)
        BigDecimal totalDeductions = rewardDisciplineRepository.sumDeductions(empId, start, end);

        // Get employee name from User entity
        String fullName = emp.getUser() != null ? emp.getUser().getFullName() : "Unknown";
        String employeeCode = emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "N/A";

        return EmployeeAggregationDTO.builder()
                .employeeId(empId)
                .employeeCode(employeeCode)
                .fullName(fullName)
                .standardDays(standardDays)
                .unpaidLeaveDays(unpaidLeaveDays)
                .payableDays(payableDays)
                .totalOvertimeHours(totalOvertimeHours)
                .totalBonuses(totalBonuses != null ? totalBonuses : BigDecimal.ZERO)
                .totalDeductions(totalDeductions != null ? totalDeductions : BigDecimal.ZERO)
                .build();
    }

    /**
     * Counts the number of weekdays (Mon-Fri) in the inclusive date range [start, end].
     */
    private int countWeekdays(LocalDate start, LocalDate end) {
        int count = 0;
        LocalDate date = start;
        while (!date.isAfter(end)) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }

    /**
     * Calculates the number of overlapping weekdays between a request range (Instant)
     * and a period range (LocalDate). Converts Instant to LocalDate in Vietnam timezone,
     * clips to the period range, then counts weekdays.
     */
    private long calculateOverlappingWeekdays(Instant reqFrom, Instant reqTo,
                                               LocalDate periodStart, LocalDate periodEnd) {
        if (reqFrom == null || reqTo == null) return 0;

        // Convert Instant to LocalDate in Vietnam timezone
        LocalDate reqStartDate = reqFrom.atZone(VIETNAM_ZONE).toLocalDate();
        LocalDate reqEndDate = reqTo.atZone(VIETNAM_ZONE).toLocalDate();

        // Clip to period range
        LocalDate effectiveStart = reqStartDate.isBefore(periodStart) ? periodStart : reqStartDate;
        LocalDate effectiveEnd = reqEndDate.isAfter(periodEnd) ? periodEnd : reqEndDate;

        if (effectiveStart.isAfter(effectiveEnd)) return 0;

        long count = 0;
        LocalDate date = effectiveStart;
        while (!date.isAfter(effectiveEnd)) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }

    /**
     * Converts a LocalDate to the start-of-day Instant in Vietnam timezone.
     */
    private Instant toStartOfDayInstant(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIN, VIETNAM_ZONE).toInstant();
    }

    /**
     * Converts a LocalDate to the end-of-day Instant in Vietnam timezone.
     */
    private Instant toEndOfDayInstant(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MAX, VIETNAM_ZONE).toInstant();
    }
}
