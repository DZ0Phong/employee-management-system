package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrEmployeeDTO;
import com.group5.ems.dto.response.HrEmployeeDetailDTO;
import com.group5.ems.entity.Contract;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Salary;
import com.group5.ems.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HrEmployeeService {

    private final EmployeeRepository employeeRepository;

    public HrEmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public List<HrEmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Page<HrEmployeeDTO> searchEmployees(String search, String department, String status, Pageable pageable) {
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String deptParam = (department != null && !department.trim().isEmpty() && !"All".equalsIgnoreCase(department.trim())) ? department.trim() : null;
        String statusParam = (status != null && !status.trim().isEmpty() && !"All".equalsIgnoreCase(status.trim())) ? status.trim() : null;

        Page<Employee> page = employeeRepository.searchEmployees(searchParam, deptParam, statusParam, pageable);
        List<HrEmployeeDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public HrEmployeeDetailDTO getEmployeeDetail(Long id) {
        Employee employee = employeeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        String initials = "";
        String fullName = "Unknown";
        String email = "N/A";
        String phone = "N/A";
        String username = "N/A";

        if (employee.getUser() != null) {
            fullName = employee.getUser().getFullName() != null ? employee.getUser().getFullName() : fullName;
            email = employee.getUser().getEmail() != null ? employee.getUser().getEmail() : email;
            phone = employee.getUser().getPhone() != null ? employee.getUser().getPhone() : phone;
            username = employee.getUser().getUsername() != null ? employee.getUser().getUsername() : username;
            if (!"Unknown".equals(fullName) && !fullName.trim().isEmpty()) {
                String[] names = fullName.trim().split("\\s+");
                initials += names[0].charAt(0);
                if (names.length > 1) {
                    initials += names[names.length - 1].charAt(0);
                }
            }
        }

        String departmentName = employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A";
        String positionName = employee.getPosition() != null ? employee.getPosition().getName() : "N/A";

        // Get latest salary
        BigDecimal baseSalary = BigDecimal.ZERO;
        BigDecimal allowance = BigDecimal.ZERO;
        String salaryType = "N/A";
        java.time.LocalDate salaryEffectiveFrom = null;

        List<Salary> salaries = employee.getSalaries();
        if (salaries != null && !salaries.isEmpty()) {
            Salary latest = salaries.stream()
                    .max(Comparator.comparing(Salary::getEffectiveFrom))
                    .orElse(null);
            if (latest != null) {
                baseSalary = latest.getBaseAmount() != null ? latest.getBaseAmount() : BigDecimal.ZERO;
                allowance = latest.getAllowanceAmount() != null ? latest.getAllowanceAmount() : BigDecimal.ZERO;
                salaryType = latest.getSalaryType() != null ? latest.getSalaryType() : "N/A";
                salaryEffectiveFrom = latest.getEffectiveFrom();
            }
        }

        // Get latest contract
        String contractType = "N/A";
        java.time.LocalDate contractStart = null;
        java.time.LocalDate contractEnd = null;
        String contractStatus = "N/A";

        List<Contract> contracts = employee.getContracts();
        if (contracts != null && !contracts.isEmpty()) {
            Contract latest = contracts.stream()
                    .max(Comparator.comparing(Contract::getStartDate))
                    .orElse(null);
            if (latest != null) {
                contractType = latest.getContractType() != null ? latest.getContractType() : "N/A";
                contractStart = latest.getStartDate();
                contractEnd = latest.getEndDate();
                contractStatus = latest.getStatus() != null ? latest.getStatus() : "N/A";
            }
        }

        return HrEmployeeDetailDTO.builder()
                .id(employee.getId())
                .initials(initials.toUpperCase())
                .fullName(fullName)
                .code(employee.getEmployeeCode())
                .department(departmentName)
                .position(positionName)
                .status(employee.getStatus())
                .hireDate(employee.getHireDate())
                .email(email)
                .phone(phone)
                .username(username)
                .baseSalary(baseSalary)
                .allowance(allowance)
                .salaryType(salaryType)
                .salaryEffectiveFrom(salaryEffectiveFrom)
                .contractType(contractType)
                .contractStart(contractStart)
                .contractEnd(contractEnd)
                .contractStatus(contractStatus)
                .build();
    }

    private HrEmployeeDTO mapToDTO(Employee employee) {
        String initials = "";
        String fullName = "Unknown";
        String email = "N/A";
        String phone = "N/A";
        if (employee.getUser() != null) {
            fullName = employee.getUser().getFullName() != null ? employee.getUser().getFullName() : fullName;
            email = employee.getUser().getEmail() != null ? employee.getUser().getEmail() : email;
            phone = employee.getUser().getPhone() != null ? employee.getUser().getPhone() : phone;
            if (!"Unknown".equals(fullName) && !fullName.trim().isEmpty()) {
                String[] names = fullName.trim().split("\\s+");
                initials += names[0].charAt(0);
                if (names.length > 1) {
                    initials += names[names.length - 1].charAt(0);
                }
            }
        }

        String departmentName = employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A";
        String positionName = employee.getPosition() != null ? employee.getPosition().getName() : "N/A";

        return HrEmployeeDTO.builder()
                .id(employee.getId())
                .initials(initials.toUpperCase())
                .fullName(fullName)
                .position(positionName)
                .department(departmentName)
                .code(employee.getEmployeeCode())
                .status(employee.getStatus())
                .email(email)
                .phone(phone)
                .build();
    }
}
