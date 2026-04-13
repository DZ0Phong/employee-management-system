package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.entity.*;
import com.group5.ems.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HRManagerDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final StaffingRequestRepository staffingRequestRepository;

    private static final String DEFAULT_AVATAR =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAx3bm_6ROku45Qad2UC6L8WqGYQTSxbQfGbrIsZyy-UW0G-0eeaUe05OzGGUPVXtUgSAXYY1km4lsQ8OMlKocQqnLvoWylgqv8HhjdOhc-kA7_Y9WGXOHncHiVIom2GDXi5UFfTRWNw-kIM5Tj5rLVJx3alhzAv1liLktNE8Zt65-kYJuInGPkWm85aD_STgeoCKnakLN1ZpxNfG-GLOhHh26_zxMgT8NQ21STEfw2DrFNb7ygWY6IQKmzRFuP-NmzVNfiEHO9zvA";

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Map<String, Object> getDepartmentsViewData() {
        Map<String, Object> data = new HashMap<>();
        User currentUser = getCurrentUser();

        // Get all departments
        List<Department> departments = departmentRepository.findAll();
        List<Map<String, Object>> departmentList = departments.stream()
                .map(this::mapDepartmentToView)
                .collect(Collectors.toList());

        data.put("departments", departmentList);
        data.put("currentUser", mapUserToView(currentUser));
        data.put("totalDepartments", departments.size());

        return data;
    }

    public Map<String, Object> getStaffingRequestsData() {
        Map<String, Object> data = new HashMap<>();
        User currentUser = getCurrentUser();

        // Get all pending staffing requests
        List<StaffingRequest> pendingRequests = staffingRequestRepository.findAllPendingRequests();
        List<Map<String, Object>> requestList = pendingRequests.stream()
                .map(this::mapStaffingRequestToView)
                .collect(Collectors.toList());

        // Get unassigned employees (employees without department)
        List<Employee> unassignedEmployees = employeeRepository.findByDepartmentIdIsNull();
        List<Map<String, Object>> availableEmployees = unassignedEmployees.stream()
                .filter(emp -> "ACTIVE".equalsIgnoreCase(emp.getStatus()))
                .map(this::mapEmployeeToView)
                .collect(Collectors.toList());

        // Get all departments for dropdown
        List<Department> departments = departmentRepository.findAll();
        List<Map<String, String>> departmentOptions = departments.stream()
                .map(dept -> {
                    Map<String, String> option = new HashMap<>();
                    option.put("id", String.valueOf(dept.getId()));
                    option.put("name", dept.getName());
                    return option;
                })
                .collect(Collectors.toList());

        data.put("staffingRequests", requestList);
        data.put("availableEmployees", availableEmployees);
        data.put("departments", departmentOptions);
        data.put("currentUser", mapUserToView(currentUser));
        data.put("pendingCount", pendingRequests.size());

        return data;
    }

    public List<UserDTO> getDepartmentMembers(Long departmentId) {
        List<Employee> employees = employeeRepository.findByDepartmentIdWithUser(departmentId);
        return employees.stream()
                .map(this::mapEmployeeToUserDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapEmployeeToUserDTO(Employee employee) {
        if (employee.getUser() == null) {
            return null;
        }
        
        User user = employee.getUser();
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : DEFAULT_AVATAR);
        
        // Get role from UserRole relationship
        String roleName = "EMPLOYEE";
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            roleName = user.getUserRoles().stream()
                .findFirst()
                .map(ur -> ur.getRole() != null ? ur.getRole().getName() : "EMPLOYEE")
                .orElse("EMPLOYEE");
        }
        dto.setRole(roleName);
        dto.setStatus(employee.getStatus());
        
        // Note: initials are computed via getInitials() method in UserDTO, no need to set
        
        return dto;
    }

    @Transactional
    public boolean assignEmployeeToDepartment(Long requestId, Long employeeId) {
        try {
            StaffingRequest request = staffingRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            if (!"PENDING".equals(request.getStatus())) {
                return false;
            }

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            // Assign employee to department
            employee.setDepartmentId(request.getDepartmentId());
            employeeRepository.save(employee);

            // Update request status
            request.setStatus("COMPLETED");
            request.setAssignedEmployeeId(employeeId);
            request.setProcessedByUserId(getCurrentUser().getId());
            request.setProcessedAt(LocalDateTime.now());
            staffingRequestRepository.save(request);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public boolean transferEmployeeBetweenDepartments(Long employeeId, Long fromDepartmentId, Long toDepartmentId) {
        try {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            if (!Objects.equals(employee.getDepartmentId(), fromDepartmentId)) {
                return false;
            }

            employee.setDepartmentId(toDepartmentId);
            employeeRepository.save(employee);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public boolean rejectStaffingRequest(Long requestId, String reason) {
        try {
            StaffingRequest request = staffingRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            if (!"PENDING".equals(request.getStatus())) {
                return false;
            }

            request.setStatus("REJECTED");
            request.setProcessedByUserId(getCurrentUser().getId());
            request.setProcessedAt(LocalDateTime.now());
            staffingRequestRepository.save(request);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> mapDepartmentToView(Department department) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", department.getId());
        map.put("name", department.getName());
        map.put("code", department.getCode());
        map.put("description", department.getDescription());
        
        // Count employees
        int employeeCount = department.getEmployees() != null ? department.getEmployees().size() : 0;
        map.put("employeeCount", employeeCount);

        // Manager info
        if (department.getManager() != null && department.getManager().getUser() != null) {
            map.put("managerName", department.getManager().getUser().getFullName());
        } else {
            map.put("managerName", "No manager");
        }

        return map;
    }

    private Map<String, Object> mapStaffingRequestToView(StaffingRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", request.getId());
        map.put("requestType", request.getRequestType());
        map.put("roleRequested", request.getRoleRequested());
        map.put("description", request.getDescription());
        map.put("status", request.getStatus());
        map.put("createdAt", request.getCreatedAt());

        // Department info
        if (request.getDepartment() != null) {
            map.put("departmentName", request.getDepartment().getName());
            map.put("departmentId", request.getDepartment().getId());
        }

        // Requester info
        if (request.getRequestedByEmployee() != null && request.getRequestedByEmployee().getUser() != null) {
            map.put("requesterName", request.getRequestedByEmployee().getUser().getFullName());
        } else {
            map.put("requesterName", "Unknown");
        }

        return map;
    }

    private Map<String, Object> mapEmployeeToView(Employee employee) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", employee.getId());
        map.put("employeeCode", employee.getEmployeeCode());
        
        if (employee.getUser() != null) {
            map.put("fullName", employee.getUser().getFullName());
            map.put("email", employee.getUser().getEmail());
            map.put("avatarUrl", employee.getUser().getAvatarUrl() != null 
                ? employee.getUser().getAvatarUrl() 
                : DEFAULT_AVATAR);
        } else {
            map.put("fullName", "Employee #" + employee.getId());
            map.put("email", "");
            map.put("avatarUrl", DEFAULT_AVATAR);
        }

        if (employee.getPosition() != null) {
            map.put("position", employee.getPosition().getName());
        } else {
            map.put("position", "No position");
        }

        map.put("status", employee.getStatus());
        map.put("hireDate", employee.getHireDate());

        return map;
    }

    private Map<String, Object> mapUserToView(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("fullName", user.getFullName());
        map.put("email", user.getEmail());
        map.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : DEFAULT_AVATAR);
        return map;
    }
}
