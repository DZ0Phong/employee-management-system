package com.group5.ems.service.common;

import com.group5.ems.dto.request.BankDetailsFormDTO;
import com.group5.ems.dto.response.BankDetailsResponseDTO;
import com.group5.ems.dto.vietqr.VietQrBankDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.EmployeeBankDetail;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.EmployeeBankDetailRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.service.external.VietQrApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class BankDetailsService {

    private final EmployeeBankDetailRepository bankDetailRepository;
    private final EmployeeRepository employeeRepository;
    private final LogService logService;
    private final VietQrApiClient vietQrApiClient;

    public Page<BankDetailsResponseDTO> getBankDetailsHistory(Long employeeId, Pageable pageable) {
        return bankDetailRepository.findByEmployeeId(employeeId, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional
    public void addBankDetails(Long employeeId, BankDetailsFormDTO dto) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        bankDetailRepository.resetPrimaryAccounts(employeeId);
        dto.setIsPrimary(true);

        // Find bank shortName from cached list
        String bankShortName = vietQrApiClient.getSupportedBanks().stream()
                .filter(b -> b.bin().equals(dto.getBankCode()))
                .map(VietQrBankDTO::shortName)
                .findFirst()
                .orElse(dto.getBankCode());

        EmployeeBankDetail detail = new EmployeeBankDetail();
        detail.setEmployee(employee);
        detail.setBankName(bankShortName);
        detail.setBranchName(null);
        detail.setAccountName(dto.getAccountName());
        detail.setAccountNumber(dto.getAccountNumber());
        detail.setIsPrimary(dto.getIsPrimary());
        
        EmployeeBankDetail saved = bankDetailRepository.save(detail);
        
        // Rule #15: Logging
        logService.log(AuditAction.CREATE, AuditEntityType.BANK_DETAILS, saved.getId());
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
