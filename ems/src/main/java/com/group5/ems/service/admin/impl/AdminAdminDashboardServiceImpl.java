package com.group5.ems.service.admin.impl;

import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Role;
import com.group5.ems.entity.User;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.repository.UserRoleRepository;
import com.group5.ems.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminAdminDashboardServiceImpl implements AdminDashboardService {
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    @Override
    public int getAllActiveEmployees() {
        return (int) userRepository.countUsersWithEmployeeByStatus("ACTIVE");
    }

    @Override
    public int getAllInactiveEmployees() {
        return (int) userRepository.countUsersWithEmployeeByStatus("INACTIVE");
    }

    @Override
    public int getAllSuspendedEmployees() {
        return (int) userRepository.countUsersWithEmployeeByStatuses(List.of("LOCKED", "LOCK5"));
    }

    @Override
    public int getAllEmployeesCount() {
        return (int) employeeRepository.count();
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
        return (int) departmentRepository.count();
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

    @Override
    public List<String> getHeadcountMonths(int months) {
        List<String> out =  new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            out.add(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)); // "Apr"
        }
        return out;
    }

    @Override
    public List<Integer> getHeadcountTotal(int months) {
        List<Integer> out = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate asOf = now.minusMonths(i).atEndOfMonth();
            out.add((int) employeeRepository.hiredDateUpTo(asOf));
        }
        return out;
    }

    @Override
    public List<Integer> getHeadcountActive(int months) {
        List<Integer> out = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate asOf = now.minusMonths(i).atEndOfMonth();
            out.add((int) employeeRepository.countHireUpToByStatus(asOf, "ACTIVE"));
        }
        return out;
    }

    @Override
    public List<Integer> getHeadcountSuspended(int months) {
        List<Integer> out = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate asOf = now.minusMonths(i).atEndOfMonth();
            out.add((int) employeeRepository.countHireUpToByStatus(asOf, "LOCKED"));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getTop5RecentUser() {
        List<User> userList = userRepository.findTop5ByOrderByCreatedAtDesc();
        return userList.stream().map(this::toUserDTO).toList();
    }
    public UserDTO toUserDTO(User user) {
        String firstName = "";
        String lastName = "";
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            String[] splitName = user.getFullName().trim().split("\\s+");
            if (splitName.length > 0) {
                firstName = splitName[splitName.length - 1];
                lastName = splitName.length > 1 ? String.join(" ", Arrays.copyOfRange(splitName, 0, splitName.length - 1)) : "";
            }
        }

        String status = user.getStatus();
        String statusDB = status != null ? status : "";
        if ("ACTIVE".equalsIgnoreCase(status)) statusDB = "Active";
        else if ("INACTIVE".equalsIgnoreCase(status)) statusDB = "Inactive";
        else if ("LOCKED".equalsIgnoreCase(status)) statusDB = "Suspended";

        Role role = user.getUserRoles() != null
                ? user.getUserRoles().stream()
                .map(com.group5.ems.entity.UserRole::getRole)
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(Role::getName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .findFirst()
                .orElse(null)
                : null;
        String roleCode = (role != null && role.getName() != null) ? role.getName() : "";
        String deptName = (user.getEmployee() != null && user.getEmployee().getDepartment() != null)
                ? user.getEmployee().getDepartment().getName() : "";

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firstName(firstName)
                .lastName(lastName)
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(statusDB)
                .isVerified(user.getIsVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .role(roleCode)
                .departmentName(deptName)
                .build();
    }
}
