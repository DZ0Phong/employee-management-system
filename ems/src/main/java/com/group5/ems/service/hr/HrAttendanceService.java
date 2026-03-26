package com.group5.ems.service.hr;

import com.group5.ems.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrAttendanceService {

    private final AttendanceRepository attendanceRepository;

    public org.springframework.data.domain.Page<com.group5.ems.dto.response.HrAttendanceDetailDTO> getAttendanceRecords(java.time.LocalDate workDate, String search, String department, String status, org.springframework.data.domain.Pageable pageable) {
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

    public com.group5.ems.dto.response.HrAttendanceStatsDTO getAttendanceStats(java.time.LocalDate workDate) {
        long presentCount = attendanceRepository.countByWorkDateAndStatus(workDate, "PRESENT");
        long lateCount = attendanceRepository.countByWorkDateAndStatus(workDate, "LATE");
        long leaveCount = attendanceRepository.countByWorkDateAndStatus(workDate, "LEAVE");
        long absentCount = attendanceRepository.countByWorkDateAndStatus(workDate, "ABSENT");
        return new com.group5.ems.dto.response.HrAttendanceStatsDTO(presentCount, lateCount, leaveCount, absentCount);
    }
}
