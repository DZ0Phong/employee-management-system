package com.group5.ems.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record RecruitmentTicketDTO(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @Email(message = "Please provide a valid email format")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Phone number is required")
        String phone,

        @NotNull(message = "Please select a department")
        Long departmentId,

        @NotNull(message = "Please select a job title")
        Long positionId,

        @NotNull(message = "Please select a joining date")
        LocalDate joiningDate,

        Long reportingManagerId,

        @NotNull(message = "Please enter an annual base salary")
        BigDecimal baseSalary,

        @NotBlank(message = "Please specify a pay frequency")
        String payFrequency,

        List<Long> bonusIds,

        String additionalNotes
) {}
