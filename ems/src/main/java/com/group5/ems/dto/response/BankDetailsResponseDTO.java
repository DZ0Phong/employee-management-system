package com.group5.ems.dto.response;

import lombok.Builder;

@Builder
public record BankDetailsResponseDTO(
    Long id,
    String bankName,
    String branchName,
    String accountName,
    String maskedAccountNumber,
    Boolean isPrimary
) {}
