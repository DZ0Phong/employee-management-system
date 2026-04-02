package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrAttendanceDetailDTO;
import com.group5.ems.dto.response.HrAttendanceStatsDTO;
import com.group5.ems.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrAttendanceService {

    private final AttendanceRepository attendanceRepository;

    public Page<HrAttendanceDetailDTO> getAttendanceRecords(LocalDate workDate, String search, String department, String status, Pageable pageable) {
        if (search != null) search = search.trim();
        if (department != null) {
            department = department.trim();
            if (department.equalsIgnoreCase("All Departments") || department.equalsIgnoreCase("All") || department.isEmpty()) {
                department = null;
            }
        }
        if (status != null) {
            status = status.trim();
            if (status.equalsIgnoreCase("Status: All") || status.equalsIgnoreCase("All") || status.isEmpty()) {
                status = null;
            } else {
               status = status.toUpperCase();
            }
        }
        return attendanceRepository.findAttendanceDetails(workDate, department, status, search, pageable);
    }

    public HrAttendanceStatsDTO getAttendanceStats(LocalDate workDate) {
        long presentCount = attendanceRepository.countByWorkDateAndStatus(workDate, "PRESENT");
        long lateCount = attendanceRepository.countByWorkDateAndStatus(workDate, "LATE");
        long leaveCount = attendanceRepository.countByWorkDateAndStatus(workDate, "LEAVE");
        long absentCount = attendanceRepository.countByWorkDateAndStatus(workDate, "ABSENT");
        return new HrAttendanceStatsDTO(presentCount, lateCount, leaveCount, absentCount);
    }
}
