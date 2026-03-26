package com.group5.ems.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankDetailsFormDTO {
    
    @NotBlank(message = "Bank selection is required")
    private String bankCode;
    
    @NotBlank(message = "Account name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Account name must contain only unaccented letters and spaces (e.g., NGUYEN VAN A)")
    @Size(min = 2, max = 100, message = "Account name must be between 2 and 100 characters")
    private String accountName;
    
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]+$", message = "Account number must contain only digits")
    @Size(min = 6, max = 20, message = "Account number must be between 6 and 20 digits")
    private String accountNumber;
    
    @Builder.Default
    private Boolean isPrimary = false;
}
