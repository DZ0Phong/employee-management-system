package com.group5.ems.service.deptmanager;

import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.User;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeptManagerUtilService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final RequestRepository requestRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    public Employee getCurrentEmployee() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return null;
        return employeeRepository.findByUserId(currentUser.getId()).orElse(null);
    }

    public Map<String, String> getManagerMap(User user) {
        Map<String, String> manager = new HashMap<>();
        if (user != null) {
            manager.put("name", user.getFullName());
            // Simplistic role derivation
            manager.put("role", "Department Manager");
            manager.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl()
                    : "https://lh3.googleusercontent.com/aida-public/AB6AXuDdPhGMiAuUVMrhgqJVFn7WwuSwOLn9a730wH2usyu4spUNv9xdN6YBAMA1bABIftIBnWGbN4ZOta3fvUqdmQRPhCb4JMMxyVITyF3CXo6Srgkl9jI21MbXolPsVUwxgExfg-52F2HuTfQ6J4o8bqtfvprk6ikOWkdiJjTsXS_uWawAu1l8WErbThrS0pmx91Dh6uDOoITamBraDKhrQ9er2LfexZBrhboZh3DcLncMTsqT9CZHa9SHfcD8lEFBXDC8zmwPLNHkISk");
        }
        return manager;
    }

    public Department getDepartmentForManager(User user) {
        if (user == null) return null;
        Employee managerEmp = employeeRepository.findByUserId(user.getId()).orElse(null);
        if (managerEmp == null) return null;

        List<Department> managedDepts = departmentRepository.findByManagerId(managerEmp.getId());
        if (!managedDepts.isEmpty()) {
            return managedDepts.get(0);
        }
        return managerEmp.getDepartment();
    }

    public Department getCurrentManagedDepartment() {
        return getDepartmentForManager(getCurrentUser());
    }

    public boolean isEmployeeInManagedDepartment(Long employeeId) {
        Department department = getCurrentManagedDepartment();
        if (department == null || employeeId == null) {
            return false;
        }
        return employeeRepository.findById(employeeId)
                .map(Employee::getDepartmentId)
                .filter(department.getId()::equals)
                .isPresent();
    }

    public Employee requireCurrentEmployee() {
        Employee employee = getCurrentEmployee();
        if (employee == null) {
            throw new IllegalStateException("Current user is not linked to an employee record.");
        }
        return employee;
    }

    public Department requireCurrentManagedDepartment() {
        Department department = getCurrentManagedDepartment();
        if (department == null) {
            throw new IllegalStateException("Current department manager is not assigned to a department.");
        }
        return department;
    }

    public int getPendingApprovalsCount(Department dept) {
        int pendingApprovals = 0;
        if (dept != null) {
            List<com.group5.ems.entity.Request> requests = requestRepository.findByEmployeeDepartmentIdAndLeaveTypeIsNotNullOrderByCreatedAtDesc(dept.getId());
            for (com.group5.ems.entity.Request req : requests) {
                if ("PENDING".equalsIgnoreCase(req.getStatus())) {
                    pendingApprovals++;
                }
            }
        }
        return pendingApprovals;
    }
}
