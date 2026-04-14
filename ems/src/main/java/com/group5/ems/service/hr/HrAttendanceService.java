package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrAttendanceDetailDTO;
import com.group5.ems.dto.response.HrAttendanceStatsDTO;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.AttendanceRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final LogService logService;

    public Page<HrAttendanceDetailDTO> getAttendanceRecords(LocalDate workDate, String search, Long departmentId, String status, Pageable pageable) {
        String[] normalized = normalizeFilters(search, status);
        return attendanceRepository.findAttendanceDetails(workDate, departmentId, normalized[1], normalized[0], pageable);
    }

    public HrAttendanceStatsDTO getAttendanceStats(LocalDate workDate) {
        long presentCount = attendanceRepository.countByWorkDateAndStatus(workDate, "PRESENT");
        long lateCount = attendanceRepository.countByWorkDateAndStatus(workDate, "LATE");
        long leaveCount = attendanceRepository.countByWorkDateAndStatus(workDate, "LEAVE");
        long absentCount = attendanceRepository.countByWorkDateAndStatus(workDate, "ABSENT");
        return new HrAttendanceStatsDTO(presentCount, lateCount, leaveCount, absentCount);
    }

    @Transactional
    public void exportAttendanceToCsv(LocalDate workDate, String search, Long departmentId, String status, PrintWriter writer) {
        String[] normalized = normalizeFilters(search, status);
        List<HrAttendanceDetailDTO> records = attendanceRepository.findAllAttendanceDetails(workDate, departmentId, normalized[1], normalized[0]);

        // CSV Header
        writer.println("Employee Name,Employee Code,Department,Work Date,Check In,Check Out,Status,Note");

        for (HrAttendanceDetailDTO record : records) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    escapeCsv(record.fullName()),
                    escapeCsv(record.employeeCode()),
                    escapeCsv(record.departmentName()),
                    record.workDate(),
                    record.checkIn() != null ? record.checkIn().toString() : "",
                    record.checkOut() != null ? record.checkOut().toString() : "",
                    escapeCsv(record.status()),
                    escapeCsv(record.note()));
        }
        writer.flush();
        
        logService.log(AuditAction.EXPORT, AuditEntityType.ATTENDANCE, null);
    }

    @Transactional
    public void exportEmployeeAttendanceToCsv(Long employeeId, java.time.YearMonth targetMonth, String search, String status, PrintWriter writer) {
        // Find employee name for header/logging (optional, we could fetch from DB to be clean)
        String[] normalized = normalizeFilters(search, status);
        
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();
        
        List<com.group5.ems.entity.Attendance> records = attendanceRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(
                employeeId, 
                startDate, 
                endDate);

        // Filter in memory for search/status if needed, but since it's just this employee we can simplify
        // or just rely on the existing projection if we made a new repo method. Let's just do a simple filter here:
        if (normalized[0] != null && !normalized[0].isEmpty()) {
            records = records.stream()
                .filter(r -> r.getNote() != null && r.getNote().toLowerCase().contains(normalized[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        if (normalized[1] != null) {
            String qStatus = normalized[1];
            records = records.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().equalsIgnoreCase(qStatus))
                .collect(Collectors.toList());
        }

        // CSV Header
        writer.println("Employee Code,Work Date,Check In,Check Out,Status,Note");

        for (com.group5.ems.entity.Attendance record : records) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    escapeCsv(record.getEmployee().getEmployeeCode()),
                    record.getWorkDate(),
                    record.getCheckIn() != null ? record.getCheckIn().toString() : "",
                    record.getCheckOut() != null ? record.getCheckOut().toString() : "",
                    escapeCsv(record.getStatus()),
                    escapeCsv(record.getNote()));
        }
        writer.flush();
        
        logService.log(AuditAction.EXPORT, AuditEntityType.ATTENDANCE, null);
    }

    private String[] normalizeFilters(String search, String status) {
        if (search != null) search = search.trim();
        if (status != null) {
            status = status.trim();
            if (status.equalsIgnoreCase("All Status") || status.equalsIgnoreCase("All") || status.isEmpty()) {
                status = null;
            } else {
                status = status.toUpperCase();
            }
        }
        return new String[]{search, status};
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
