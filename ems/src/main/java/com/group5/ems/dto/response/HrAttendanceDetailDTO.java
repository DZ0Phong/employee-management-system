package com.group5.ems.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record HrAttendanceDetailDTO(
    Long id,
    String employeeCode,
    String fullName,
    String initials,
    String avatarUrl,
    String departmentName,
    LocalDate workDate,
    LocalTime checkIn,
    LocalTime checkOut,
    String status,
    String note
) {
    public HrAttendanceDetailDTO(Long id, String employeeCode, String fullName, String avatarUrl, String departmentName, LocalDate workDate, LocalTime checkIn, LocalTime checkOut, String status, String note) {
        this(id, employeeCode, fullName, calculateInitials(fullName), avatarUrl, departmentName, workDate, checkIn, checkOut, status, note);
    }

    private static String calculateInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "JD";
        String[] names = fullName.trim().split("\\s+");
        String init = "";
        if (names.length > 0 && !names[0].isEmpty()) {
            init += names[0].charAt(0);
        }
        if (names.length > 1 && !names[names.length - 1].isEmpty()) {
            init += names[names.length - 1].charAt(0);
        } else if (names.length == 1 && names[0].length() > 1) {
             init += names[0].charAt(1);
        }
        return init.toUpperCase();
    }
}
