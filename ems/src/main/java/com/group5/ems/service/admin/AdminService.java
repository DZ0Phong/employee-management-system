package com.group5.ems.service.admin;

import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Role;
import com.group5.ems.entity.User;
import com.group5.ems.repository.*;
import com.group5.ems.repository.spec.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public List<User> findAll(){
        return userRepository.findAll();
    }

    public Optional<User> getCurrentUser(){
        Authentication auth =  SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated()){
            return null;
        }
        return userRepository.findByUsername(auth.getName());
    }

    public List<Role> findAllRoles(){
        return roleRepository.findAll();
    }

    public Role getRoleByUserId(Long id){
        return userRoleRepository.getRoleByUserId(id);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersFilter(String keyword,
                                        String roleFilter,
                                        String statusFilter,
                                        String sortFilter,
                                        String sortDir,
                                        int page,
                                        int pageSize) {
        // page = chỉ số trang (0-based), pageSize = số dòng mỗi trang
        if (pageSize < 1) pageSize = 10;
        if (page < 0) page = 0;

        // Chỉ chấp nhận fullName hoặc lastLogin; mặc định fullName
        if (sortFilter == null || sortFilter.isEmpty()
                || (!"fullName".equals(sortFilter) && !"role".equals(sortFilter) && !"lastLogin".equals(sortFilter))) {
            sortFilter = "fullName";
        }
        if (sortDir == null || sortDir.isEmpty()
                || (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir))) {
            sortDir = "asc";
        }

        String entitySortField = "lastLogin".equals(sortFilter) ? "lastLoginAt" : "fullName".equals(sortFilter) ? "fullName" : "fullName";
        Sort.Direction sortDirection = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(sortDirection, entitySortField);

        Pageable pageable = PageRequest.of(page, pageSize, sort);
        Specification<User> spec = UserSpecification.withFilters(keyword, statusFilter, roleFilter);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        return userPage.map(this::toUserDTO);
    }

     public long getStatusTotal(){
        return userRepository.count();
     }
    public long getStatusActive(){
        return userRepository.countByStatus("ACTIVE");
     }
    public long getStatusInactive(){
        return userRepository.countByStatus("INACTIVE");
     }
    public long getStatusSuspended(){
        return userRepository.countByStatus("LOCKED");
     }

     public List<String> getDepartmentName(){
        return departmentRepository.findAll().stream().map(Department::getName).toList();
     }

     public Optional<UserDTO> getUserDTO(){
        Authentication auth =  SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated()){
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if(userOpt.isEmpty()){
            return Optional.empty();
        }
        User user = userOpt.get();
        return Optional.of(toUserDTO(user));
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

        Role role = userRoleRepository.getRoleByUserId(user.getId());
        String roleCode = (role != null && role.getCode() != null) ? role.getCode() : "";
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
