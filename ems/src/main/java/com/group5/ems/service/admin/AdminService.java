package com.group5.ems.service.admin;

import com.group5.ems.dto.request.DepartmentFormDTO;
import com.group5.ems.dto.request.SaveUserRequest;
import com.group5.ems.dto.response.DepartmentDTO;
import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.entity.*;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.*;
import com.group5.ems.repository.spec.UserSpecification;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    @Transactional
    public void saveDepartment(DepartmentFormDTO form) {
        if (form == null) {
            throw new IllegalArgumentException("Department data is required");
        }

        String name = form.getName() != null ? form.getName().trim() : "";
        String code = form.getCode() != null ? form.getCode().trim() : "";

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Department name cannot be empty");
        }
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Department code cannot be empty");
        }

        // enforce uppercase; keep it consistent even if UI forgets
        code = code.toUpperCase();

        Long id = form.getId();
        Long parentId = form.getParentId(); // null = root
        if (parentId != null && parentId.equals(id)) {
            throw new IllegalArgumentException("Parent department cannot be itself");
        }

        // Unique code check
        if (id == null) {
            if (departmentRepository.existsByCode(code)) {
                throw new IllegalArgumentException("Department code already exists");
            }
        } else {
            if (departmentRepository.existsByCodeAndIdNot(code, id)) {
                throw new IllegalArgumentException("Department code already exists");
            }
        }

        boolean isCreate = (id == null);
        Department department;
        if (id == null) {
            department = new Department();
        } else {
            department = departmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        }

        department.setName(name);
        department.setCode(code);
        department.setDescription(isBlank(form.getDescription()) ? null : form.getDescription().trim());
        department.setParentId(parentId); // null = root
        department.setManagerId(form.getManagerId()); // nullable ok

        departmentRepository.save(department);
        logService.log(isCreate ? AuditAction.CREATE : AuditAction.UPDATE,
                AuditEntityType.DEPARTMENT,
                department.getId());
    }

    @Transactional
    public void deleteDepartment(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Department id is required");
        }

        // ensure exists
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        // block delete if has employees
        int staffCount = employeeRepository.countByDepartmentId(id);
        if (staffCount > 0) {
            throw new IllegalArgumentException("Cannot delete: department has members");
        }

        // block delete if has children
        List<Department> children = departmentRepository.findByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete: department has child departments");
        }

        departmentRepository.delete(dept);
        logService.log(AuditAction.DELETE, AuditEntityType.DEPARTMENT, id);
    }
    private final List<String> departmentSortList = List.of("name", "code" ,"createdAt");

    public long getAllDepartmentsCount()
    {
        return departmentRepository.count();
    }
    public long getAllEmployeesCount()
    {
        return employeeRepository.count();
    }
    public long getAllParents(){
        return departmentRepository.countAllParentId();
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartmentsDTO()
    {
        List<Department> dept = departmentRepository.findAll();
        return dept.stream().map(this::toDepartmentDTO).toList();
    }

    @Transactional(readOnly = true)
    public Page<DepartmentDTO> getDepartmentsFilter(String keyword,
                                              String sortField,
                                              String sortDir,
                                              int page,
                                              int pageSize){
        if(pageSize < 1) pageSize = 10;
        if(page < 0) page = 0;

        //check sort field
        if(!departmentSortList.contains(sortField)){
            sortField = "name";
        }

        //check sort dỉr
        if (sortDir == null || (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc"))) {
            sortDir = "asc";
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, pageSize, sort);
        Page<Department> departmentPage = Page.empty();
        if(keyword == null || keyword.isEmpty()){
            departmentPage = departmentRepository.findAll(pageable);
        }
        else{
           departmentPage = departmentRepository.findByNameContainingIgnoreCaseOrCodeIgnoreCase(keyword,keyword,pageable);
        }

        Page<DepartmentDTO> dept = departmentPage.map(this::toDepartmentDTO);
        return dept;
    }

    @Transactional(readOnly = true)
    public DepartmentDTO toDepartmentDTO(Department department){
        Department parent = department.getParent();
        String parentName = "";
        if(parent != null && parent.getName() != null){
            parentName = parent.getName();
        }


        String managerName = "";
        String managerImgUrl = "";

        Employee manager = department.getManager();
        if(manager != null && manager.getUser() != null){
            User user = manager.getUser();
            managerName = user.getFullName();
            managerImgUrl = user.getAvatarUrl();
        }
        int staffCount = employeeRepository.countByDepartmentId(department.getId());


        return DepartmentDTO
                .builder()
                .code(department.getCode())
                .name(department.getName())
                .description(department.getDescription())
                .id(department.getId())
                .parentId(department.getParentId())
                .parentName(parentName)
                .managerId(department.getManagerId())
                .managerName(managerName)
                .managerAvatarUrl(managerImgUrl)
                .staffCount(staffCount)
                .createTime(department.getCreatedAt())
                .updateTime(department.getUpdatedAt())
        .build();
    }

    @Transactional
    public void saveUser(SaveUserRequest req){
        if(req.getUsername().isBlank() ||req.getEmail().isBlank() ||req.getFullName().isBlank()){
            throw new IllegalArgumentException("User name, email and full name are required");
        }
        String password = req.getPassword() == null ? "" : req.getPassword().trim();
        //add user
        if(req.getId() == null) {
            if (password.isBlank()) {
                throw new IllegalArgumentException("Password is required");
            }
            if (password.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }
            if (userRepository.findByEmail(req.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email is already exist");
            }
            if (userRepository.findByUsername(req.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Username is already exist");
            }
            User user = new User();
            applyCommonFields(user, req);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setAvatarUrl(null);
            userRepository.save(user);
            createEmployeeProfileIfMissing(user);
            // Gán role được chọn cho user mới (nếu có)
            assignRole(user, req.getRole());
            logService.log(AuditAction.CREATE, AuditEntityType.USER, user.getId());
        }
        //edit
        else{
            User existingUser = userRepository.findById(req.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (userRepository.existsByEmailAndIdNot(req.getEmail(), req.getId())) {
                throw new IllegalArgumentException("Email is already exist");
            }
            if (userRepository.existsByUsernameAndIdNot(req.getUsername(), req.getId())) {
                throw new IllegalArgumentException("Username is already exist");
            }
            applyCommonFields(existingUser, req);
            if(!password.isBlank()){
                if (password.length() < 6) {
                    throw new IllegalArgumentException("Password must be at least 6 characters");
                }
                existingUser.setPasswordHash(passwordEncoder.encode(password));
            }
            userRepository.save(existingUser);
            // Cập nhật lại role cho user (xóa cũ, gán mới nếu có)
            assignRole(existingUser, req.getRole());
            logService.log(AuditAction.UPDATE, AuditEntityType.USER, existingUser.getId());
        }
    }


    private void applyCommonFields(User user, SaveUserRequest req) {
        user.setUsername(req.getUsername().trim());
        user.setEmail(req.getEmail().trim());
        user.setFullName(req.getFullName().trim());
        user.setPhone(isBlank(req.getPhone()) ? null : req.getPhone().trim());
        user.setStatus(mapStatus(req.getStatus())); // Active -> ACTIVE, ...
    }

    /**
     * Gán một role (theo roleCode) cho user.
     * Nếu roleCode rỗng/null -> không làm gì.
     * Hiện tại mỗi user chỉ giữ 1 role nên sẽ xóa hết role cũ trước khi gán mới.
     */
    private void assignRole(User user, String roleCode) {
        if (isBlank(roleCode)) {
            return;
        }
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + roleCode));

        // Xóa các mapping role cũ của user (nếu có)
        userRoleRepository.deleteByUserId(user.getId());

        // Tạo mapping mới
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleRepository.save(userRole);
    }

    private void createEmployeeProfileIfMissing(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        if (employeeRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }
        Position defaultPosition = findDefaultPosition();
        Employee employee = new Employee();
        employee.setUserId(user.getId());
        employee.setEmployeeCode(generateEmployeeCode(user.getId()));
        employee.setPositionId(defaultPosition.getId());
        employee.setHireDate(LocalDate.now());
        employee.setStatus("ACTIVE");
        employeeRepository.save(employee);
        logService.log(AuditAction.CREATE, AuditEntityType.EMPLOYEE, employee.getId());
    }

    private Position findDefaultPosition() {
        return positionRepository.findByCode("EMPLOYEE")
                .or(() -> positionRepository.findByCode("STAFF"))
                .or(() -> positionRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot create employee profile: no position configured"));
    }

    private String generateEmployeeCode(Long userId) {
        return String.format("EMP-%06d", userId);
    }

    private String mapStatus(String uiStatus) {
        if ("Active".equalsIgnoreCase(uiStatus))       return "ACTIVE";
        if ("Inactive".equalsIgnoreCase(uiStatus))     return "INACTIVE";
        if ("Locked".equalsIgnoreCase(uiStatus))       return "LOCKED";   // admin lock
        if ("Lock5".equalsIgnoreCase(uiStatus))        return "LOCK5";    // brute-force
        throw new IllegalArgumentException("Invalid status: " + uiStatus);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
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

    /**
     * Danh sách user có thể chọn làm Department Manager trong form.
     * Hiện tại filter theo role code = DEPT_MANAGER (đúng với SecurityConfig).
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllManagersForSelect() {
        Optional<Role> managerRoleOpt = roleRepository.findByCode("DEPT_MANAGER");
        if (managerRoleOpt.isEmpty()) {
            return List.of();
        }

        Role managerRole = managerRoleOpt.get();
        List<UserRole> mappings = userRoleRepository.findByRoleId(managerRole.getId());
        if (mappings == null || mappings.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = mappings.stream()
                .map(UserRole::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return List.of();
        }

        return userRepository.findAllById(userIds).stream()
                .map(this::toUserDTO)
                .toList();
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
                                        int pageSize,
                                        String departmentFilter) {
        if (pageSize < 1) pageSize = 10;
        if (page < 0) page = 0;

        if (sortFilter == null || sortFilter.isEmpty()
                || (!"fullName".equals(sortFilter) && !"role".equals(sortFilter) && !"lastLogin".equals(sortFilter))) {
            sortFilter = "fullName";
        }
        if (sortDir == null || sortDir.isEmpty()
                || (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir))) {
            sortDir = "asc";
        }

        String entitySortField = "lastLogin".equals(sortFilter) ? "lastLoginAt" : "fullName";
        Sort.Direction sortDirection = Sort.Direction.fromString(sortDir);
        Sort sort = Sort.by(sortDirection, entitySortField);

        Pageable pageable = PageRequest.of(page, pageSize, sort);
        Specification<User> spec = UserSpecification.withFilters(keyword, statusFilter, roleFilter, departmentFilter);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        return userPage.map(this::toUserDTO);
    }

    /** Overload giữ backward-compat với code cũ */
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersFilter(String keyword,
                                        String roleFilter,
                                        String statusFilter,
                                        String sortFilter,
                                        String sortDir,
                                        int page,
                                        int pageSize) {
        return getUsersFilter(keyword, roleFilter, statusFilter, sortFilter, sortDir, page, pageSize, null);
    }

     public long getStatusTotal(){
        return userRepository.count();
     }
     public long getStatusActive(){
        return userRepository.countUsersWithEmployeeByStatus("ACTIVE");
     }
    public long getStatusInactive(){
        return userRepository.countUsersWithEmployeeByStatus("INACTIVE");
     }
    /** Đếm tất cả tài khoản đang bị khoá (cả LOCK5 brute-force lẫn LOCKED admin). */
    public long getStatusLocked() {
        return userRepository.countUsersWithEmployeeByStatuses(List.of("LOCKED", "LOCK5"));
    }

    /** Đếm riêng brute-force lock (LOCK5). */
    public long getStatusLock5() {
        return userRepository.countUsersWithEmployeeByStatus("LOCK5");
    }

    /** Đếm riêng admin lock (LOCKED). */
    public long getStatusAdminLocked() {
        return userRepository.countUsersWithEmployeeByStatus("LOCKED");
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
        if ("ACTIVE".equalsIgnoreCase(status))        statusDB = "Active";
        else if ("INACTIVE".equalsIgnoreCase(status)) statusDB = "Inactive";
        else if ("LOCKED".equalsIgnoreCase(status))   statusDB = "Locked";   // admin lock
        else if ("LOCK5".equalsIgnoreCase(status))    statusDB = "Lock5";    // brute-force

        Role role = user.getUserRoles() != null
                ? user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(Role::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
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
                .failedLoginCount(user.getFailedLoginCount())
                .build();
    }
}
