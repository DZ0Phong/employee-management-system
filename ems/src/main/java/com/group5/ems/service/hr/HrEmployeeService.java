package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrEmployeeDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

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

    private HrEmployeeDTO mapToDTO(Employee employee) {
        String initials = "";
        String fullName = "Unknown";
        if (employee.getUser() != null) {
            fullName = employee.getUser().getFullName() != null ? employee.getUser().getFullName() : fullName;
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
                .build();
    }
}
