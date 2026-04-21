package com.group5.ems.controller.admin;

import com.group5.ems.dto.request.SaveUserRequest;
import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.entity.Role;
import com.group5.ems.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UserListController {

    private final AdminService adminService;



    @GetMapping("/users")
    public String users(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Model model) {

        adminService.getUserDTO().ifPresent(u -> model.addAttribute("currentUser", u));

        model.addAttribute("keyword",          keyword);
        model.addAttribute("roleFilter",       role);
        model.addAttribute("statusFilter",     status);
        model.addAttribute("departmentFilter", department);
        model.addAttribute("sortField",        sort);
        model.addAttribute("sortDir",          dir);

        model.addAttribute("roles",           adminService.findAllRoles());
        model.addAttribute("departments",     adminService.getDepartmentName());
        model.addAttribute("departmentCount", adminService.getAllDepartmentsCount());
        model.addAttribute("statTotal",       adminService.getStatusTotal());
        model.addAttribute("statActive",      adminService.getStatusActive());
        model.addAttribute("statInactive",    adminService.getStatusInactive());
        model.addAttribute("statLock5",       adminService.getStatusLock5());       // LOCK5: brute-force
        model.addAttribute("statAdminLocked", adminService.getStatusAdminLocked()); // LOCKED: admin

        Page<UserDTO> users = adminService.getUsersFilter(keyword, role, status, sort, dir, page, pageSize, department);
        model.addAttribute("users",         users);
        model.addAttribute("currentPage",   users.getNumber());
        model.addAttribute("pageSize",      users.getSize());
        model.addAttribute("totalPages",    users.getTotalPages());
        model.addAttribute("totalElements", users.getTotalElements());
        model.addAttribute("saveUserRequest", new SaveUserRequest());

        return "admin/user-list";
    }

    /**
     * Form fields must use the prefix {@code saveUserRequest.*} so Spring binds into the DTO
     * (see {@code ServletRequestDataBinder} + object name). Plain {@code name="password"} would not bind.
     */
    @PostMapping("/users/save")
    public String saveUser(@Valid @ModelAttribute("saveUserRequest") SaveUserRequest saveUserRequest,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String error = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("message", error);
            return "redirect:/admin/users";
        }
        try {
            adminService.saveUser(saveUserRequest);
            redirectAttributes.addFlashAttribute("message", "User has been saved successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Create one or more accounts by email only.
     * Password is auto-generated and sent to each email address.
     * Accounts are created as INACTIVE by default.
     */
    @PostMapping("/users/create-by-email")
    public String createUsersByEmail(
            @RequestParam("emails") String emails,
            @RequestParam(value = "role", defaultValue = "") String role,
            RedirectAttributes redirectAttributes) {
        try {
            String result = adminService.createUsersByEmail(emails, role);
            redirectAttributes.addFlashAttribute("message", result);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/admin/users";
    }




}
