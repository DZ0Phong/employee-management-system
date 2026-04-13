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

    private String[] normalizeFilters(String search, String status) {
        if (search != null) search = search.trim();
        if (status != null) {
            status = status.trim();
            if (status.equalsIgnoreCase("Status: All") || status.equalsIgnoreCase("All") || status.isEmpty()) {
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
