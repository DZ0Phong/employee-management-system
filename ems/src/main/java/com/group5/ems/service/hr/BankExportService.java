package com.group5.ems.service.hr;

import com.group5.ems.dto.hr.BankExportRowDTO;
import com.group5.ems.entity.EmployeeBankDetail;
import com.group5.ems.entity.Payslip;
import com.group5.ems.repository.BankExportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankExportService {

    private final BankExportRepository bankExportRepository;

    /**
     * Generates CSV content for bank export based on a payroll period.
     *
     * @param periodId the payroll period ID
     * @return the raw CSV content as a string
     * @throws IllegalStateException if no approved payslips with primary bank details are found
     */
    @Transactional(readOnly = true)
    public String generateCsvContent(Long periodId) {
        List<Object[]> results = bankExportRepository.findApprovedPayslipsWithPrimaryBankDetails(periodId);

        if (results.isEmpty()) {
            throw new IllegalStateException("No approved payslips with primary bank details found for this period.");
        }

        List<BankExportRowDTO> exportRows = results.stream().map(result -> {
            Payslip p = (Payslip) result[0];
            EmployeeBankDetail b = (EmployeeBankDetail) result[1];
            return new BankExportRowDTO(
                p.getEmployee().getEmployeeCode(),
                p.getEmployee().getUser().getFullName(),
                b.getBankName(),
                b.getBranchName(),
                b.getAccountName(),
                b.getAccountNumber(), // JPA Converter automatically decrypts this
                p.getNetSalary()
            );
        }).collect(Collectors.toList());

        return buildCsv(exportRows);
    }

    private String buildCsv(List<BankExportRowDTO> rows) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("\"Employee Code\",\"Employee Name\",\"Bank Name\",\"Branch\",\"Account Name\",\"Account Number\",\"Net Salary Amount\"\n");

        // Rows
        for (BankExportRowDTO row : rows) {
            sb.append("\"").append(escapeCsv(row.employeeCode())).append("\",");
            sb.append("\"").append(escapeCsv(row.fullName())).append("\",");
            sb.append("\"").append(escapeCsv(row.bankName())).append("\",");
            sb.append("\"").append(escapeCsv(row.branchName())).append("\",");
            sb.append("\"").append(escapeCsv(row.accountName())).append("\",");
            sb.append("\"").append(escapeCsv(row.accountNumber())).append("\",");
            sb.append(row.netSalary().toPlainString()).append("\n"); // plain number format
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
