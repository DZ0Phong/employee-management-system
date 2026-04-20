package com.group5.ems.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record HrEmployeeDetailDTO(
        // Info
        Long id,
        String initials,
        String avatarUrl,
        String fullName,
        String code,
        String department,
        String position,
        String status,
        LocalDate hireDate,

        // Personal
        String email,
        String phone,
        String username,

        // Salary
        BigDecimal baseSalary,
        BigDecimal allowance,
        String salaryType,
        LocalDate salaryEffectiveFrom,

        // Contract
        String contractType,
        LocalDate contractStart,
        LocalDate contractEnd,
        String contractStatus,

        // Reports & Records
        java.util.List<HrEmployeePerformanceDTO> performanceReviews,
        java.util.List<HrEmployeeDisciplineDTO> disciplines
) {
}