package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrAttendanceDTO;
import com.group5.ems.entity.Attendance;
import com.group5.ems.repository.AttendanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HrAttendanceService {

    private final AttendanceRepository attendanceRepository;

    public HrAttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<HrAttendanceDTO> getAllAttendances() {
        return attendanceRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private HrAttendanceDTO mapToDTO(Attendance attendance) {
        String initials = "";
        String fullName = "Unknown";
        String departmentName = "N/A";

        if (attendance.getEmployee() != null) {
            if (attendance.getEmployee().getUser() != null) {
                fullName = attendance.getEmployee().getUser().getFullName() != null ? attendance.getEmployee().getUser().getFullName() : fullName;
                if (!"Unknown".equals(fullName) && !fullName.trim().isEmpty()) {
                    String[] names = fullName.trim().split("\\s+");
                    initials += names[0].charAt(0);
                    if (names.length > 1) {
                        initials += names[names.length - 1].charAt(0);
                    }
                }
            }
            if (attendance.getEmployee().getDepartment() != null) {
                departmentName = attendance.getEmployee().getDepartment().getName();
            }
        }

        return HrAttendanceDTO.builder()
                .id(attendance.getId())
                .employeeName(fullName)
                .initials(initials.toUpperCase())
                .department(departmentName)
                .workDate(attendance.getWorkDate())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .status(attendance.getStatus())
                .build();
    }
}
