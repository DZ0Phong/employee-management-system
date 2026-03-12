package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfileDTO {
    // Từ users
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String status;

    // Từ employees
    private Long employeeId;
    private String employeeCode;
    private LocalDate hireDate;

    // Join từ departments & positions
    private String departmentName;
    private String positionName;
}