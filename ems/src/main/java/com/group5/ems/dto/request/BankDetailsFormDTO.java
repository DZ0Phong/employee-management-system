package com.group5.ems.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankDetailsFormDTO {
    
    @NotBlank(message = "Bank name is required")
    private String bankName;
    
    private String branchName;
    
    @NotBlank(message = "Account holder name is required")
    private String accountName;
    
    @NotBlank(message = "Account number is required")
    private String accountNumber;
    
    @Builder.Default
    private Boolean isPrimary = false;
}
