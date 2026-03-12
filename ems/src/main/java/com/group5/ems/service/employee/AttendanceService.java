package com.group5.ems.service.employee;

import com.group5.ems.dto.response.AttendanceDTO;
import com.group5.ems.dto.response.AttendanceStatsDTO;

import java.util.List;

public interface AttendanceService {
    List<AttendanceDTO> getAttendanceHistory(Long employeeId, int year, int month);
    AttendanceStatsDTO getAttendanceStats(Long employeeId, int year, int month);
    void clockIn(Long employeeId);
    void clockOut(Long employeeId);
    byte[] exportReport(Long employeeId, int year, int month);
}