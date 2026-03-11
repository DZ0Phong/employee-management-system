package com.group5.ems.service.admin.impl;

import com.group5.ems.entity.Department;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAdminDashboardServiceImpl implements AdminDashboardService {
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public int getAllActiveEmployees() {
        return employeeRepository.findByStatus("ACTIVE").size();
    }

    @Override
    public int getAllInactiveEmployees() {
        return employeeRepository.findByStatus("INACTIVE").size();
    }

    @Override
    public int getAllSuspendedEmployees() {
        return employeeRepository.findByStatus("LOCKED").size();
    }

    @Override
    public int getAllEmployeesCount() {
        return employeeRepository.findAll().size();
    }

    @Override
    public long getNewThisMonth() {
        return employeeRepository.newThisMonth();
    }

    @Override
    public Double getActiveRate() {
        int total = getAllEmployeesCount();
        return total == 0 ? 0.0 : Math.round(getAllActiveEmployees() * 100.0 / total * 10) / 10.0;
    }

    @Override
    public Double getInactiveRate() {
        int total = getAllEmployeesCount();
        return total == 0 ? 0.0 : Math.round(getAllInactiveEmployees() * 100.0 / total * 10) / 10.0;
    }

    @Override
    public Double getSuspendedRate() {
        int total = getAllEmployeesCount();
        return total == 0 ? 0.0 : Math.round(getAllSuspendedEmployees() * 100.0 / total * 10) / 10.0;
    }

    @Override
    public long getNewThisYear() {
        return employeeRepository.newThisYear();
    }

    @Override
    public int getAllDepartmentsCount() {
        return departmentRepository.findAll().size();
    }

    @Override
    public List<String> getAllDepartmentsName() {
        List<Department> department = departmentRepository.findAll();
        return department.stream().map(Department::getName).toList();
    }

    @Override
    public List<Object[]> getAllDepartmentsPercentage() {
        return employeeRepository.countEmployeeByDepartmentName();
    }
}
