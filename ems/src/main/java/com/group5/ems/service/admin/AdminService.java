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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
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

    private static final int    TEMP_PWD_LENGTH  = 12;
    private static final String CHARS_UPPER   = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String CHARS_LOWER   = "abcdefghjkmnpqrstuvwxyz";
    private static final String CHARS_DIGITS  = "23456789";
    private static final String CHARS_SPECIAL = "@#$%&*!";
    private static final String CHARS_ALL     = CHARS_UPPER + CHARS_LOWER + CHARS_DIGITS + CHARS_SPECIAL;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender  mailSender;
    private final LogService logService;

    @Value("${spring.mail.username}")
    private String mailFrom;

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
        Employee employee = new Employee();
        employee.setUserId(user.getId());
        employee.setEmployeeCode(generateEmployeeCode(user.getId()));
        employee.setPositionId(null);
        employee.setHireDate(LocalDate.now());
        employee.setStatus("ACTIVE");
        employeeRepository.save(employee);
        logService.log(AuditAction.CREATE, AuditEntityType.EMPLOYEE, employee.getId());
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

    // ── Email-based bulk user creation ───────────────────────────────────────

    /**
     * Create one or more accounts from a raw string of emails (comma / newline separated).
     * Each account is created as INACTIVE with an auto-generated password sent by email.
     * Returns a human-readable summary string.
     */
    @Transactional
    public String createUsersByEmail(String rawEmails, String roleCode) {
        if (isBlank(rawEmails)) {
            throw new IllegalArgumentException("At least one email address is required");
        }

        String[] parts = rawEmails.split("[,;\\n\\r]+");
        List<String> created        = new ArrayList<>();
        List<String> emailFailed    = new ArrayList<>();
        List<String> failed         = new ArrayList<>();

        for (String part : parts) {
            String email = part.trim().toLowerCase();
            if (email.isEmpty()) continue;

            try {
                String tempPwd = generateTempPassword();
                boolean emailSent = createSingleUserByEmail(email, roleCode, tempPwd);
                if (emailSent) {
                    created.add(email);
                } else {
                    emailFailed.add(email);
                }
            } catch (Exception e) {
                String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                failed.add(email + " (" + reason + ")");
            }
        }

        if (created.isEmpty() && emailFailed.isEmpty() && failed.isEmpty()) {
            throw new IllegalArgumentException("No valid email addresses found");
        }

        StringBuilder sb = new StringBuilder();
        if (!created.isEmpty()) {
            sb.append(created.size()).append(" account(s) created — credentials sent to email");
        }
        if (!emailFailed.isEmpty()) {
            if (sb.length() > 0) sb.append(". ");
            sb.append(emailFailed.size()).append(" account(s) created but email delivery failed: ")
              .append(String.join(", ", emailFailed));
        }
        if (!failed.isEmpty()) {
            if (sb.length() > 0) sb.append(". ");
            sb.append(failed.size()).append(" skipped: ").append(String.join("; ", failed));
        }
        return sb.toString();
    }

    /**
     * Creates a single user account. Returns true if the welcome email was sent successfully,
     * false if the account was created but the email could not be delivered.
     * Throws IllegalArgumentException if the account itself could not be created.
     */
    private boolean createSingleUserByEmail(String email, String roleCode, String tempPassword) {
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("invalid email format");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email already exists");
        }

        String username = generateUsernameFromEmail(email);
        String fullName = deriveFullNameFromEmail(email);

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setStatus("INACTIVE");
        user.setAvatarUrl(null);
        userRepository.save(user);

        createEmployeeProfileIfMissing(user);
        assignRole(user, roleCode);
        logService.log(AuditAction.CREATE, AuditEntityType.USER, user.getId());

        try {
            sendWelcomeEmail(email, fullName, username, tempPassword);
            return true;
        } catch (Exception ex) {
            String err = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            System.err.println("[AdminService] Welcome email failed for " + email + ": " + err);
            return false;
        }
    }

    private String generateUsernameFromEmail(String email) {
        String base = email.toLowerCase().split("@")[0];
        base = base.replaceAll("[^a-z0-9._]", "");
        if (base.isEmpty()) base = "user";
        while (base.length() < 6) base = base + "0";
        if (base.length() > 45) base = base.substring(0, 45);

        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter++;
        }
        return username;
    }

    private String deriveFullNameFromEmail(String email) {
        String prefix = email.split("@")[0];
        String[] tokens = prefix.split("[._\\-]+");
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(t.charAt(0)));
            if (t.length() > 1) sb.append(t.substring(1).toLowerCase());
        }
        return sb.length() > 0 ? sb.toString() : prefix;
    }

    private String generateTempPassword() {
        SecureRandom rng = new SecureRandom();
        char[] pwd = new char[TEMP_PWD_LENGTH];
        pwd[0] = CHARS_UPPER.charAt(rng.nextInt(CHARS_UPPER.length()));
        pwd[1] = CHARS_LOWER.charAt(rng.nextInt(CHARS_LOWER.length()));
        pwd[2] = CHARS_DIGITS.charAt(rng.nextInt(CHARS_DIGITS.length()));
        pwd[3] = CHARS_SPECIAL.charAt(rng.nextInt(CHARS_SPECIAL.length()));
        for (int i = 4; i < TEMP_PWD_LENGTH; i++) {
            pwd[i] = CHARS_ALL.charAt(rng.nextInt(CHARS_ALL.length()));
        }
        for (int i = TEMP_PWD_LENGTH - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = pwd[i]; pwd[i] = pwd[j]; pwd[j] = tmp;
        }
        return new String(pwd);
    }

    private void sendWelcomeEmail(String toEmail, String fullName, String username, String tempPassword)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        helper.setFrom(mailFrom);
        helper.setTo(toEmail);
        helper.setSubject("EMS Pro — Your Account Has Been Created");

        String safePwd  = escapeHtml(tempPassword);
        String safeName = escapeHtml(fullName);

        String html =
            "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/></head>" +
            "<body style=\"font-family:Arial,sans-serif;background:#f6f6f8;margin:0;padding:24px;\">" +
            "<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"max-width:560px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 16px rgba(0,0,0,.08);\">" +
            "<tr><td style=\"padding:28px 32px 12px;text-align:center;\">" +
            "<div style=\"display:inline-block;padding:8px 20px;background:#1414b8;border-radius:10px;margin-bottom:12px;\">" +
            "<span style=\"color:#fff;font-size:16px;font-weight:900;letter-spacing:.05em;\">EMS Pro</span></div>" +
            "<h2 style=\"margin:8px 0 0;font-size:18px;color:#0f172a;\">Your account is ready</h2>" +
            "</td></tr>" +
            "<tr><td style=\"padding:16px 32px 8px;\">" +
            "<p style=\"font-size:14px;color:#374151;margin:0;\">Hi <strong>" + safeName + "</strong>,</p>" +
            "<p style=\"font-size:14px;color:#374151;margin:12px 0 0;\">An administrator has created an account for you on <strong>EMS Pro</strong>. Here are your login credentials:</p>" +
            "</td></tr>" +
            "<tr><td style=\"padding:12px 32px;\">" +
            "<table width=\"100%\" style=\"background:#f8fafc;border-radius:10px;border:1px solid #e2e8f0;\">" +
            "<tr><td style=\"padding:14px 20px;border-bottom:1px solid #e2e8f0;\">" +
            "<span style=\"font-size:11px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:.08em;\">Login Email</span><br/>" +
            "<span style=\"font-size:14px;font-weight:600;color:#1e293b;\">" + escapeHtml(toEmail) + "</span>" +
            "</td></tr>" +
            "<tr><td style=\"padding:14px 20px;\">" +
            "<span style=\"font-size:11px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:.08em;\">Temporary Password</span><br/>" +
            "<span style=\"display:inline-block;margin-top:6px;padding:10px 24px;background:#1414b8;color:#fff;font-size:18px;font-weight:700;letter-spacing:.15em;border-radius:9999px;\">" +
            safePwd + "</span>" +
            "</td></tr></table>" +
            "</td></tr>" +
            "<tr><td style=\"padding:12px 32px 28px;\">" +
            "<p style=\"font-size:13px;color:#6b7280;margin:0;\">Your account is currently <strong>inactive</strong>. An administrator will activate it shortly.</p>" +
            "<p style=\"font-size:13px;color:#6b7280;margin:12px 0 0;\">Once active, please log in and <strong>change your password immediately</strong>.</p>" +
            "<p style=\"font-size:12px;color:#9ca3af;margin:12px 0 0;\">If you did not expect this email, please contact your system administrator.</p>" +
            "</td></tr>" +
            "</table></body></html>";

        helper.setText(html, true);
        mailSender.send(message);
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }

    // ─────────────────────────────────────────────────────────────────────────

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
