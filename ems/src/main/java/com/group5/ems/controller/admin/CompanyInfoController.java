package com.group5.ems.controller.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.admin.AdminService;
import com.group5.ems.service.guest.CompanyService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/company-info")
@RequiredArgsConstructor
public class CompanyInfoController {

    private final CompanyService companyService;
    private final AdminService adminService;
    private final UserRepository userRepository;

    // ─── List ────────────────────────────────────────────────────────
    @GetMapping
    public String list(Model model) {
        model.addAttribute("companyInfoList", companyService.getAllCompanyInfo());
        // Sidebar & topbar — đồng bộ với DashBoardController / UserListController
        adminService.getUserDTO().ifPresent(u -> model.addAttribute("currentUser", u));
        model.addAttribute("departmentCount", adminService.getAllDepartmentsCount());
        return "admin/company-info";
    }

    // ─── Create ──────────────────────────────────────────────────────
    @PostMapping("/create")
    public String create(
            @RequestParam String infoKey,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "false") boolean isPublic,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {

        try {
            Long actorId = resolveUserId(principal);
            companyService.createCompanyInfo(infoKey.trim(), title.trim(), content.trim(), isPublic, actorId);
            ra.addFlashAttribute("successMsg", "Da tao muc \"" + title + "\" thanh cong.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Loi he thong: " + e.getMessage());
        }
        return "redirect:/admin/company-info";
    }

    // ─── Update ──────────────────────────────────────────────────────
    @PostMapping("/update")
    public String update(
            @RequestParam Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "false") boolean isPublic,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {

        try {
            Long actorId = resolveUserId(principal);
            companyService.updateCompanyInfo(id, title.trim(), content.trim(), isPublic, actorId);
            ra.addFlashAttribute("successMsg", "Da cap nhat muc \"" + title + "\".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Loi: " + e.getMessage());
        }
        return "redirect:/admin/company-info";
    }

    // ─── Delete ──────────────────────────────────────────────────────
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            companyService.deleteCompanyInfo(id);
            ra.addFlashAttribute("successMsg", "Da xoa muc thanh cong.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Khong the xoa: " + e.getMessage());
        }
        return "redirect:/admin/company-info";
    }
    
    private Long resolveUserId(UserDetails principal) {
        if (principal == null)
            return null;
        return userRepository.findByUsername(principal.getUsername())
                .map(u -> u.getId())
                .orElse(null);
    }
}