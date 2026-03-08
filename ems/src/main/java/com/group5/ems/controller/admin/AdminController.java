package com.group5.ems.controller.admin;

import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.entity.Role;
import com.group5.ems.entity.User;
import com.group5.ems.repository.RoleRepository;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.repository.UserRoleRepository;
import com.group5.ems.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @ResponseBody
    public String dashboard() {
        return "Admin dashboard - OK (role ADMIN)";
    }

    @GetMapping("/users")
    public String users(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Model model) {
        //get current user
        adminService.getUserDTO().ifPresent(u -> model.addAttribute("currentUser", u));
        //search
        model.addAttribute("keyword",       keyword);
        model.addAttribute("roleFilter",    role);
        model.addAttribute("statusFilter",  status);
        model.addAttribute("sortField",     sort);
        model.addAttribute("sortDir",       dir);

        //get all role
        List<Role> roles = adminService.findAllRoles();
        model.addAttribute("roles", roles);

        //get dept
        model.addAttribute("departments",adminService.getDepartmentName());

        model.addAttribute("statTotal", adminService.getStatusTotal());
        model.addAttribute("statActive", adminService.getStatusActive());
        model.addAttribute("statInactive", adminService.getStatusInactive());
        model.addAttribute("statSuspended", adminService.getStatusSuspended());

        model.addAttribute("departments", adminService.getDepartmentName());

        //paging
        Page<UserDTO> users = adminService.getUsersFilter(keyword, role, status, sort, dir, page, pageSize);
        model.addAttribute("users", users);
        model.addAttribute("currentPage", users.getNumber());
        model.addAttribute("pageSize", users.getSize());
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("totalElements", users.getTotalElements());


        return "admin/user-list";
    }
}
