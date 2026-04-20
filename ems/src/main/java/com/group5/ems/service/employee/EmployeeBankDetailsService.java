package com.group5.ems.service.employee;

import com.group5.ems.dto.request.BankDetailsFormDTO;
import com.group5.ems.dto.response.BankDetailsResponseDTO;
import com.group5.ems.dto.vietqr.VietQrBankDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.EmployeeBankDetail;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.EmployeeBankDetailRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.service.common.LogService;
import com.group5.ems.service.external.VietQrApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeBankDetailsService {

    private final EmployeeBankDetailRepository bankDetailRepository;
    private final EmployeeRepository employeeRepository;
    private final LogService logService;
    private final VietQrApiClient vietQrApiClient;

    @Transactional(readOnly = true)
    public List<BankDetailsResponseDTO> getBankDetails(Long employeeId) {
        return bankDetailRepository.findByEmployeeId(employeeId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BankDetailsResponseDTO getPrimaryBankDetail(Long employeeId) {
        return bankDetailRepository.findByEmployeeId(employeeId).stream()
                .filter(detail -> Boolean.TRUE.equals(detail.getIsPrimary()))
                .findFirst()
                .map(this::toResponseDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<VietQrBankDTO> getSupportedBanks() {
        try {
            return vietQrApiClient.getSupportedBanks();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    @Transactional
    public void addBankDetails(Long employeeId, BankDetailsFormDTO dto) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        boolean shouldBePrimary = bankDetailRepository.countByEmployeeId(employeeId) == 0 || Boolean.TRUE.equals(dto.getIsPrimary());
        if (shouldBePrimary) {
            bankDetailRepository.resetPrimaryAccounts(employeeId);
        }

        String bankShortName = getSupportedBanks().stream()
                .filter(bank -> bank.bin().equals(dto.getBankCode()))
                .map(VietQrBankDTO::shortName)
                .findFirst()
                .orElse(dto.getBankCode());

        EmployeeBankDetail detail = new EmployeeBankDetail();
        detail.setEmployee(employee);
        detail.setBankName(bankShortName);
        detail.setBranchName(null);
        detail.setAccountName(normalizeAccountName(dto.getAccountName()));
        detail.setAccountNumber(normalizeAccountNumber(dto.getAccountNumber()));
        detail.setIsPrimary(shouldBePrimary);

        EmployeeBankDetail saved = bankDetailRepository.save(detail);
        logService.log(AuditAction.CREATE, AuditEntityType.BANK_DETAILS, saved.getId());
    }

    @Transactional
    public void setPrimaryAccount(Long employeeId, Long bankId) {
        EmployeeBankDetail detail = bankDetailRepository.findByIdAndEmployeeId(bankId, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));

        bankDetailRepository.resetPrimaryAccounts(employeeId);
        detail.setIsPrimary(true);
        bankDetailRepository.save(detail);
        logService.log(AuditAction.UPDATE, AuditEntityType.BANK_DETAILS, bankId);
    }

    private BankDetailsResponseDTO toResponseDTO(EmployeeBankDetail entity) {
        return BankDetailsResponseDTO.builder()
                .id(entity.getId())
                .bankName(entity.getBankName())
                .branchName(entity.getBranchName())
                .accountName(entity.getAccountName())
                .maskedAccountNumber(maskAccountNumber(entity.getAccountNumber()))
                .isPrimary(entity.getIsPrimary())
                .build();
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "******" + accountNumber.substring(accountNumber.length() - 4);
    }

    private String normalizeAccountName(String accountName) {
        return accountName == null ? null : accountName.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeAccountNumber(String accountNumber) {
        return accountNumber == null ? null : accountNumber.trim();
    }
}
