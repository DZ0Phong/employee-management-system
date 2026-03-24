package com.group5.ems.service.hr;

import com.group5.ems.dto.response.BankDetailsResponseDTO;
import com.group5.ems.entity.EmployeeBankDetail;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.EmployeeBankDetailRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrBankDetailsService {

    private final EmployeeBankDetailRepository bankDetailRepository;
    private final LogService logService;

    public List<BankDetailsResponseDTO> getMaskedBankDetails(Long employeeId) {
        return bankDetailRepository.findByEmployeeId(employeeId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void setPrimaryAccount(Long employeeId, Long bankId) {
        EmployeeBankDetail detail = bankDetailRepository.findByIdAndEmployeeId(bankId, employeeId)
                .orElseThrow(() -> new RuntimeException("Bank detail not found or not owned by employee"));

        bankDetailRepository.resetPrimaryAccounts(employeeId);
        detail.setIsPrimary(true);
        bankDetailRepository.save(detail);
        
        // Rule #15: Logging
        logService.log(AuditAction.UPDATE, AuditEntityType.BANK_DETAILS, bankId);
    }

    @Transactional
    public void deleteBankDetail(Long employeeId, Long bankId) {
        EmployeeBankDetail detail = bankDetailRepository.findByIdAndEmployeeId(bankId, employeeId)
                .orElseThrow(() -> new RuntimeException("Bank detail not found or not owned by employee"));

        if (Boolean.TRUE.equals(detail.getIsPrimary())) {
            throw new RuntimeException("Cannot delete primary bank account. Set another as primary first.");
        }

        bankDetailRepository.delete(detail);
        
        // Rule #15: Logging
        logService.log(AuditAction.DELETE, AuditEntityType.BANK_DETAILS, bankId);
    }

    private BankDetailsResponseDTO toResponseDTO(EmployeeBankDetail entity) {
        String masked = maskAccountNumber(entity.getAccountNumber());
        return BankDetailsResponseDTO.builder()
                .id(entity.getId())
                .bankName(entity.getBankName())
                .branchName(entity.getBranchName())
                .accountName(entity.getAccountName())
                .maskedAccountNumber(masked)
                .isPrimary(entity.getIsPrimary())
                .build();
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "******" + accountNumber.substring(accountNumber.length() - 4);
    }
}
