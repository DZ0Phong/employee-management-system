package com.group5.ems.service.hr;

import com.group5.ems.dto.response.*;
import com.group5.ems.entity.HrReport;
import com.group5.ems.repository.*;
import com.group5.ems.service.common.LogService;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrReportService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final RequestRepository requestRepository;
    private final SalaryRepository salaryRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final HrReportRepository hrReportRepository;
    private final org.thymeleaf.TemplateEngine templateEngine;
    private final LogService logService;
    private final com.group5.ems.service.common.EmailNotificationService emailNotificationService;
    private final HrBackblazeStorageService hrBackblazeStorageService;

    // To prevent circular dependency, we can use ObjectProvider or just inject them. 
    // Given they don't depend on HrReportService, standard injection is fine.
    private final org.springframework.context.ApplicationContext applicationContext;

    private static final String REPORT_BASE_DIR = "uploads/reports/hr/";

    // ═══════════════════════════════════════════════════════════
    // REPORT PERSISTENCE & WORKFLOW
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public HrGeneratedReportDTO saveReportDraft(String tab, Integer year, LocalDate from, LocalDate to, String title, String remarks, Long employeeId) {
        // 1. Generate PDF Content
        byte[] pdfContent = generatePdfBytes(tab, year, from, to, remarks);

        // 2. Upload to Backblaze B2 (New Standard)
        String cloudFilename = "reports/hr/report_" + System.currentTimeMillis() + ".pdf";
        String filePath;
        try {
            filePath = hrBackblazeStorageService.uploadReport(cloudFilename, pdfContent);
        } catch (Exception e) {
            // Backup/Fallback: Save to local filesystem if cloud upload fails during transition 
            // OR if user specifically wants to keep local copy. 
            // Based on rules, we prioritize cloud but maintain fallback ability.
            String localFilename = "report_" + System.currentTimeMillis() + ".pdf";
            filePath = REPORT_BASE_DIR + localFilename;
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get(filePath), pdfContent);
            } catch (java.io.IOException ex) {
                throw new RuntimeException("Failed to save report even to local storage: " + ex.getMessage());
            }
        }

        // 3. Persist Metadata
        HrReport report = new HrReport();
        report.setTitle(title);
        report.setReportType(tab.toUpperCase());
        report.setFormat("PDF");
        report.setFilePath(filePath);
        report.setStatus("DRAFT");
        report.setRemarks(remarks);
        report.setGeneratedById(employeeId);
        hrReportRepository.save(report);

        logService.log(AuditAction.EXPORT, AuditEntityType.HR_REPORTS, report.getId());

        return convertToDTO(report);
    }

    @Transactional
    public void publishReport(Long reportId) {
        HrReport report = hrReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        
        report.setStatus("FINALIZED");
        report.setPublished(true);
        report.setPublishedAt(LocalDateTime.now());
        hrReportRepository.save(report);

        // Notify HR Managers
        emailNotificationService.sendReportPublishedNotification(report);

        logService.log(AuditAction.UPDATE, AuditEntityType.HR_REPORTS, report.getId());
    }

    public List<HrGeneratedReportDTO> getAllReports() {
        return hrReportRepository.findAllByOrderByGeneratedAtDesc().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public byte[] getReportFileBytes(Long reportId) {
        HrReport report = hrReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        
        String path = report.getFilePath();
        
        // Cloud Storage Retrieval (Mandatory)
        return hrBackblazeStorageService.downloadReport(path)
                .orElseThrow(() -> new RuntimeException("Report file could not be found in cloud storage (Key: " + path + ")"));
    }

    private byte[] generatePdfBytes(String tab, Integer year, LocalDate from, LocalDate to, String remarks) {
        int reportYear = year != null ? year : LocalDate.now().getYear();
        org.thymeleaf.context.Context ctx = new org.thymeleaf.context.Context();
        ctx.setVariable("selectedYear", reportYear);
        ctx.setVariable("activeTab", tab);
        ctx.setVariable("remarks", remarks); // Executive Summary for PDF
        ctx.setVariable("exportDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        switch (tab) {
            case "attendance" -> {
                LocalDate f = from != null ? from : LocalDate.now().minusDays(29);
                LocalDate t = to != null ? to : LocalDate.now();
                ctx.setVariable("dateFrom", f);
                ctx.setVariable("dateTo", t);
                ctx.setVariable("report", getAttendanceReport(f, t));
            }
            case "leave" -> ctx.setVariable("report", getLeaveReport(reportYear));
            case "payroll" -> ctx.setVariable("report", getPayrollReport());
            case "performance" -> ctx.setVariable("report", getPerformanceReport(null));
            default -> ctx.setVariable("report", getOverviewReport(reportYear));
        }

        String html = templateEngine.process("hr/reports-pdf", ctx);

        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private HrGeneratedReportDTO convertToDTO(HrReport report) {
        return HrGeneratedReportDTO.builder()
                .id(report.getId())
                .title(report.getTitle())
                .reportType(report.getReportType())
                .format(report.getFormat())
                .status(report.getStatus())
                .remarks(report.getRemarks())
                .isPublished(report.isPublished())
                .generatedAt(report.getGeneratedAt())
                .publishedAt(report.getPublishedAt())
                .generatedByName(report.getGeneratedBy() != null ? report.getGeneratedBy().getUser().getFullName() : "System")
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 1. OVERVIEW REPORT
    // ═══════════════════════════════════════════════════════════

    public HrReportOverviewDTO getOverviewReport(int year) {
        long activeCount = employeeRepository.countByStatus("ACTIVE");
        long terminatedCount = employeeRepository.countByStatus("TERMINATED");
        long onLeaveCount = employeeRepository.countByStatus("ON_LEAVE");
        long totalHeadcount = activeCount + terminatedCount + onLeaveCount;

        // Current month hires/terminations
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        long newHiresThisMonth = employeeRepository.countNewHiresInMonth(currentYear, currentMonth);
        long terminationsThisMonth = employeeRepository.countTerminationsInMonth(currentYear, currentMonth);

        // Turnover rate = (terminations in year / avg headcount) * 100
        long totalTermsInYear = 0;
        for (int m = 1; m <= 12; m++) {
            totalTermsInYear += employeeRepository.countTerminationsInMonth(year, m);
        }
        Double turnoverRate = totalHeadcount > 0
                ? Math.round(((double) totalTermsInYear / totalHeadcount) * 10000.0) / 100.0
                : 0.0;

        // Average tenure (days for active employees)
        Double avgTenureDays = null;
        // We'll compute a simple estimate based on available data

        // Department breakdown
        List<Object[]> deptRows = employeeRepository.countActiveByDepartment();
        List<HrReportOverviewDTO.DeptCount> departmentBreakdown = new ArrayList<>();
        for (Object[] row : deptRows) {
            departmentBreakdown.add(HrReportOverviewDTO.DeptCount.builder()
                    .name((String) row[0])
                    .count((Long) row[1])
                    .build());
        }

        // Monthly hiring & termination trend (12 months of selected year)
        List<String> monthlyLabels = new ArrayList<>();
        List<Long> monthlyHires = new ArrayList<>();
        List<Long> monthlyTerminations = new ArrayList<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM");
        for (int m = 1; m <= 12; m++) {
            monthlyLabels.add(Month.of(m).name().substring(0, 3));
            monthlyHires.add(employeeRepository.countNewHiresInMonth(year, m));
            monthlyTerminations.add(employeeRepository.countTerminationsInMonth(year, m));
        }

        return HrReportOverviewDTO.builder()
                .totalHeadcount(totalHeadcount)
                .activeCount(activeCount)
                .terminatedCount(terminatedCount)
                .onLeaveCount(onLeaveCount)
                .avgTenureDays(avgTenureDays)
                .newHiresThisMonth(newHiresThisMonth)
                .terminationsThisMonth(terminationsThisMonth)
                .turnoverRate(turnoverRate)
                .departmentBreakdown(departmentBreakdown)
                .monthlyLabels(monthlyLabels)
                .monthlyHires(monthlyHires)
                .monthlyTerminations(monthlyTerminations)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 2. ATTENDANCE REPORT
    // ═══════════════════════════════════════════════════════════

    public HrReportAttendanceDTO getAttendanceReport(LocalDate from, LocalDate to) {
        long total = attendanceRepository.countByWorkDateBetween(from, to);
        long present = attendanceRepository.countByStatusAndWorkDateBetween("PRESENT", from, to);
        long late = attendanceRepository.countByStatusAndWorkDateBetween("LATE", from, to);
        long absent = Math.max(0, total - present - late);

        double presentRate = total > 0 ? Math.round(((double) present / total) * 10000.0) / 100.0 : 0;
        double lateRate = total > 0 ? Math.round(((double) late / total) * 10000.0) / 100.0 : 0;
        double absentRate = total > 0 ? Math.round(((double) absent / total) * 10000.0) / 100.0 : 0;

        // Daily trend
        List<String> dailyLabels = new ArrayList<>();
        List<Long> dailyPresent = new ArrayList<>();
        List<Long> dailyLate = new ArrayList<>();
        List<Long> dailyAbsent = new ArrayList<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");

        long daysBetween = ChronoUnit.DAYS.between(from, to) + 1;
        for (long i = 0; i < daysBetween; i++) {
            LocalDate day = from.plusDays(i);
            dailyLabels.add(day.format(dayFmt));
            long dayTotal = attendanceRepository.countByWorkDateBetween(day, day);
            long dayPresent = attendanceRepository.countByStatusAndWorkDateBetween("PRESENT", day, day);
            long dayLate = attendanceRepository.countByStatusAndWorkDateBetween("LATE", day, day);
            dailyPresent.add(dayPresent);
            dailyLate.add(dayLate);
            dailyAbsent.add(Math.max(0, dayTotal - dayPresent - dayLate));
        }

        return HrReportAttendanceDTO.builder()
                .presentRate(presentRate)
                .lateRate(lateRate)
                .absentRate(absentRate)
                .totalRecords(total)
                .presentCount(present)
                .lateCount(late)
                .absentCount(absent)
                .dailyLabels(dailyLabels)
                .dailyPresent(dailyPresent)
                .dailyLate(dailyLate)
                .dailyAbsent(dailyAbsent)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 3. LEAVE REPORT
    // ═══════════════════════════════════════════════════════════

    public HrReportLeaveDTO getLeaveReport(int year) {
        LocalDateTime yearStart = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime yearEnd = LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);

        long totalApproved = requestRepository.countLeaveByStatusBetween("APPROVED", yearStart, yearEnd);
        long totalRejected = requestRepository.countLeaveByStatusBetween("REJECTED", yearStart, yearEnd);
        long totalPending = requestRepository.countByStatusAndRequestTypeCategory("PENDING", "ATTENDANCE");
        Double avgProcessingHours = requestRepository.avgProcessingHoursBetween(yearStart, yearEnd);

        // Leave type breakdown
        List<Object[]> typeRows = requestRepository.findTopLeaveTypes();
        List<String> leaveTypeLabels = new ArrayList<>();
        List<Long> leaveTypeCounts = new ArrayList<>();
        for (Object[] row : typeRows) {
            leaveTypeLabels.add((String) row[0]);
            leaveTypeCounts.add((Long) row[1]);
        }

        // Monthly trend
        List<String> monthlyLabels = new ArrayList<>();
        List<Long> monthlyApproved = new ArrayList<>();
        List<Long> monthlyRejected = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            monthlyLabels.add(Month.of(m).name().substring(0, 3));
            LocalDateTime mStart = LocalDate.of(year, m, 1).atStartOfDay();
            LocalDateTime mEnd = LocalDate.of(year, m, 1).withDayOfMonth(
                    LocalDate.of(year, m, 1).lengthOfMonth()).atTime(LocalTime.MAX);
            monthlyApproved.add(requestRepository.countLeaveByStatusBetween("APPROVED", mStart, mEnd));
            monthlyRejected.add(requestRepository.countLeaveByStatusBetween("REJECTED", mStart, mEnd));
        }

        return HrReportLeaveDTO.builder()
                .totalApproved(totalApproved)
                .totalRejected(totalRejected)
                .totalPending(totalPending)
                .avgProcessingHours(avgProcessingHours != null ? Math.round(avgProcessingHours * 10.0) / 10.0 : null)
                .leaveTypeLabels(leaveTypeLabels)
                .leaveTypeCounts(leaveTypeCounts)
                .monthlyLabels(monthlyLabels)
                .monthlyApproved(monthlyApproved)
                .monthlyRejected(monthlyRejected)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 4. PAYROLL REPORT
    // ═══════════════════════════════════════════════════════════

    public HrReportPayrollDTO getPayrollReport() {
        Double avgSalary = salaryRepository.getAverageSalary();
        BigDecimal totalCost = salaryRepository.sumCurrentPayrollCost();

        // Salary band distribution (reuse existing query)
        List<Object[]> bandRows = salaryRepository.countBySalaryBand();
        List<String> salaryBandLabels = new ArrayList<>();
        List<Long> salaryBandCounts = new ArrayList<>();
        // Define order for display
        String[] orderedBands = {"<50k", "50-80k", "80-110k", "110-150k", "150k+"};
        Map<String, Long> bandMap = new LinkedHashMap<>();
        for (String b : orderedBands) bandMap.put("$" + b, 0L);
        for (Object[] row : bandRows) {
            String label = "$" + (String) row[0];
            Long count = (Long) row[1];
            bandMap.put(label, count);
        }
        salaryBandLabels.addAll(bandMap.keySet());
        salaryBandCounts.addAll(bandMap.values());

        // Department average salary
        List<Object[]> deptRows = salaryRepository.avgSalaryByDepartment();
        List<String> deptAvgLabels = new ArrayList<>();
        List<Double> deptAvgValues = new ArrayList<>();
        for (Object[] row : deptRows) {
            deptAvgLabels.add((String) row[0]);
            deptAvgValues.add(((Number) row[1]).doubleValue());
        }

        return HrReportPayrollDTO.builder()
                .avgSalary(avgSalary != null ? Math.round(avgSalary * 100.0) / 100.0 : 0.0)
                .totalPayrollCost(totalCost != null ? totalCost : BigDecimal.ZERO)
                .salaryBandLabels(salaryBandLabels)
                .salaryBandCounts(salaryBandCounts)
                .deptAvgLabels(deptAvgLabels)
                .deptAvgValues(deptAvgValues)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 5. PERFORMANCE REPORT
    // ═══════════════════════════════════════════════════════════

    public HrReportPerformanceDTO getPerformanceReport(String reviewPeriod) {
        // Average scores
        List<Object[]> avgRows = performanceReviewRepository.getAvgScores();
        Double avgPerf = null;
        Double avgPot = null;
        if (!avgRows.isEmpty() && avgRows.get(0) != null) {
            Object[] row = avgRows.get(0);
            if (row[0] != null) avgPerf = ((Number) row[0]).doubleValue();
            if (row[1] != null) avgPot = ((Number) row[1]).doubleValue();
        }

        long totalReviews = performanceReviewRepository.countByStatus("COMPLETED");

        // Performance grade distribution
        List<Object[]> gradeRows = performanceReviewRepository.countByPerformanceGradeGrouped();
        Map<String, Long> performanceGradeDistribution = new LinkedHashMap<>();
        // Initialize with default order
        performanceGradeDistribution.put("A", 0L);
        performanceGradeDistribution.put("B", 0L);
        performanceGradeDistribution.put("C", 0L);
        performanceGradeDistribution.put("D", 0L);
        performanceGradeDistribution.put("F", 0L);

        for (Object[] row : gradeRows) {
            if (row[0] != null) {
                performanceGradeDistribution.put((String) row[0], (Long) row[1]);
            }
        }

        // Score distribution (histogram buckets)
        List<String> scoreLabels = List.of("0-1", "1-2", "2-3", "3-4", "4-5");
        List<Long> scoreCounts = new ArrayList<>();
        // Simple placeholder — actual implementation would need a dedicated query
        for (int i = 0; i < 5; i++) {
            scoreCounts.add(0L);
        }

        // Top performers
        List<Object[]> topRows = performanceReviewRepository.findTopPerformers(PageRequest.of(0, 10));
        List<HrReportPerformanceDTO.TopPerformer> topPerformers = new ArrayList<>();
        for (Object[] row : topRows) {
            topPerformers.add(HrReportPerformanceDTO.TopPerformer.builder()
                    .name((String) row[0])
                    .department(row[1] != null ? (String) row[1] : "—")
                    .score((BigDecimal) row[2])
                    .build());
        }

        return HrReportPerformanceDTO.builder()
                .avgPerformanceScore(avgPerf != null ? Math.round(avgPerf * 100.0) / 100.0 : null)
                .avgPotentialScore(avgPot != null ? Math.round(avgPot * 100.0) / 100.0 : null)
                .totalReviews(totalReviews)
                .performanceGradeDistribution(performanceGradeDistribution)
                .scoreLabels(scoreLabels)
                .scoreCounts(scoreCounts)
                .topPerformers(topPerformers)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 6. CUSTOM REPORT BUILDER
    // ═══════════════════════════════════════════════════════════

    public void exportCustomReport(String dataSource, List<String> columns, LocalDate dateFrom, LocalDate dateTo, String format, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> headers = getHeadersForCustomReport(dataSource, columns);

        if ("employees".equalsIgnoreCase(dataSource)) {
            HrEmployeeService empService = applicationContext.getBean(HrEmployeeService.class);
            org.springframework.data.domain.Page<HrEmployeeDTO> page = empService.searchEmployees(null, null, null, PageRequest.of(0, 10000));
            for (HrEmployeeDTO emp : page.getContent()) {
                // Date filtering is skipped for employees as hireDate is not available in HrEmployeeDTO
                rows.add(extractEmployeeColumns(emp, columns));
            }
        } else if ("leave".equalsIgnoreCase(dataSource)) {
            HrLeaveService leaveService = applicationContext.getBean(HrLeaveService.class);
            org.springframework.data.domain.Page<HrLeaveRequestDTO> page = leaveService.getLeaveHistoryFiltered(null, null, null, null, dateFrom, dateTo, PageRequest.of(0, 10000));
            for (HrLeaveRequestDTO leave : page.getContent()) {
                rows.add(extractLeaveColumns(leave, columns));
            }
        } else if ("attendance".equalsIgnoreCase(dataSource)) {
            HrAttendanceService attService = applicationContext.getBean(HrAttendanceService.class);
            // Since attendance might be huge, we'll fetch day by day or just use DateFrom/DateTo if provided.
            LocalDate queryFrom = dateFrom != null ? dateFrom : LocalDate.now().minusDays(30);
            LocalDate queryTo = dateTo != null ? dateTo : LocalDate.now();
            // Fetch for all days in range
            for (LocalDate day = queryFrom; !day.isAfter(queryTo); day = day.plusDays(1)) {
                org.springframework.data.domain.Page<HrAttendanceDetailDTO> page = attService.getAttendanceRecords(day, null, null, null, PageRequest.of(0, 10000));
                for (HrAttendanceDetailDTO att : page.getContent()) {
                    rows.add(extractAttendanceColumns(att, columns));
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid data source");
        }

        if ("csv".equalsIgnoreCase(format)) {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"custom_report_" + dataSource + ".csv\"");
            java.io.PrintWriter writer = response.getWriter();
            writer.write('\ufeff'); // BOM for Excel
            writer.println(String.join(",", headers.stream().map(this::escapeCsv).toList()));
            for (List<String> row : rows) {
                writer.println(String.join(",", row.stream().map(this::escapeCsv).toList()));
            }
        } else if ("pdf".equalsIgnoreCase(format)) {
            boolean limitReached = rows.size() > 1000;
            if (limitReached) {
                rows = rows.subList(0, 1000); // Limit to prevent OOM
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"custom_report_" + dataSource + ".pdf\"");
            
            org.thymeleaf.context.Context ctx = new org.thymeleaf.context.Context();
            ctx.setVariable("title", "Custom Report: " + dataSource.toUpperCase());
            ctx.setVariable("exportDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            ctx.setVariable("headers", headers);
            ctx.setVariable("rows", rows);
            ctx.setVariable("limitReached", limitReached);
            
            String html = templateEngine.process("hr/custom-report-pdf", ctx);
            
            try (java.io.OutputStream os = response.getOutputStream()) {
                org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
                renderer.setDocumentFromString(html);
                renderer.layout();
                renderer.createPDF(os);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String s = value.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    private List<String> getHeadersForCustomReport(String dataSource, List<String> columns) {
        List<String> headers = new ArrayList<>();
        Map<String, String> columnMap = switch (dataSource) {
            case "employees" -> Map.of(
                    "empCode", "Employee Code", "fullName", "Full Name",
                    "email", "Email", "department", "Department",
                    "position", "Position", "hireDate", "Hire Date", "status", "Status");
            case "leave" -> Map.of(
                    "empCode", "Employee Code", "fullName", "Full Name",
                    "department", "Department", "leaveType", "Leave Type",
                    "startDate", "Start Date", "endDate", "End Date",
                    "totalDays", "Total Days", "status", "Status", "reason", "Reason");
            case "attendance" -> Map.of(
                    "empCode", "Employee Code", "fullName", "Full Name",
                    "department", "Department", "workDate", "Work Date",
                    "clockIn", "Clock In", "clockOut", "Clock Out",
                    "status", "Status", "notes", "Notes");
            default -> Map.of();
        };

        for (String col : columns) {
            headers.add(columnMap.getOrDefault(col, col));
        }
        return headers;
    }

    private List<String> extractEmployeeColumns(HrEmployeeDTO emp, List<String> columns) {
        List<String> row = new ArrayList<>();
        for (String col : columns) {
            row.add(switch (col) {
                case "empCode" -> emp.code();
                case "fullName" -> emp.fullName();
                case "email" -> emp.email();
                case "phone" -> emp.phone();
                case "department" -> emp.department();
                case "position" -> emp.position();
                case "status" -> emp.status();
                default -> "";
            });
        }
        return row;
    }

    private List<String> extractLeaveColumns(HrLeaveRequestDTO leave, List<String> columns) {
        List<String> row = new ArrayList<>();
        for (String col : columns) {
            row.add(switch (col) {
                case "empCode" -> leave.employeeCode();
                case "fullName" -> leave.employeeName();
                case "department" -> leave.department();
                case "leaveType" -> leave.leaveType();
                case "startDate" -> leave.leave_from() != null ? leave.leave_from().toString() : "";
                case "endDate" -> leave.leave_to() != null ? leave.leave_to().toString() : "";
                case "totalDays" -> leave.duration() != null ? leave.duration() : "";
                case "status" -> leave.status();
                case "reason" -> leave.reason();
                case "submittedAt" -> leave.submittedAtDisplay();
                case "processedAt" -> leave.processedAt() != null ? leave.processedAt().toString() : "";
                case "approverName" -> leave.approverName() != null ? leave.approverName() : "";
                default -> "";
            });
        }
        return row;
    }

    private List<String> extractAttendanceColumns(HrAttendanceDetailDTO att, List<String> columns) {
        List<String> row = new ArrayList<>();
        for (String col : columns) {
            row.add(switch (col) {
                case "empCode" -> att.employeeCode();
                case "fullName" -> att.fullName();
                case "department" -> att.departmentName();
                case "workDate" -> att.workDate() != null ? att.workDate().toString() : "";
                case "clockIn" -> att.checkIn() != null ? att.checkIn().toString() : "";
                case "clockOut" -> att.checkOut() != null ? att.checkOut().toString() : "";
                case "status" -> att.status();
                case "notes" -> att.note();
                default -> "";
            });
        }
        return row;
    }
}
