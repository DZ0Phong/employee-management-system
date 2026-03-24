package com.group5.ems.service.hr;

import com.group5.ems.dto.response.BankDetailsResponseDTO;
import com.group5.ems.entity.EmployeeBankDetail;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.exception.BankDetailNotFoundException;
import com.group5.ems.repository.EmployeeBankDetailRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrBankDetailsService {

    private final EmployeeBankDetailRepository bankDetailRepository;
    private final LogService logService;

    /**
     * Returns a paginated list of masked bank details for a given employee.
     */
    @Transactional(readOnly = true)
    public Page<BankDetailsResponseDTO> getPaginatedMaskedBankDetails(Long employeeId, Pageable pageable) {
        return bankDetailRepository.findByEmployeeId(employeeId, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional
    public void setPrimaryAccount(Long employeeId, Long bankId) {
        EmployeeBankDetail detail = bankDetailRepository.findByIdAndEmployeeId(bankId, employeeId)
                .orElseThrow(() -> new BankDetailNotFoundException(bankId, employeeId));

        bankDetailRepository.resetPrimaryAccounts(employeeId);
        detail.setIsPrimary(true);
        bankDetailRepository.save(detail);
        
        // Rule #15: Logging
        logService.log(AuditAction.UPDATE, AuditEntityType.BANK_DETAILS, bankId);
    }

    @Transactional
    public void deleteBankDetail(Long employeeId, Long bankId) {
        EmployeeBankDetail detail = bankDetailRepository.findByIdAndEmployeeId(bankId, employeeId)
                .orElseThrow(() -> new BankDetailNotFoundException(bankId, employeeId));

        if (Boolean.TRUE.equals(detail.getIsPrimary())) {
            throw new IllegalStateException("Cannot delete primary bank account. Set another as primary first.");
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
