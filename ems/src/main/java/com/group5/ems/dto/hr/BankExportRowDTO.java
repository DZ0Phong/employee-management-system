package com.group5.ems.dto.hr;

import java.math.BigDecimal;

/**
 * DTO for Bank Export CSV structure.
 */
public record BankExportRowDTO(
    String employeeCode,
    String fullName,
    String bankName,
    String branchName,
    String accountName,
    String accountNumber,
    BigDecimal netSalary
) {}
