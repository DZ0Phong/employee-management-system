package com.group5.ems.controller.admin;

import com.group5.ems.service.admin.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Xử lý các hành động quản lý tài khoản của admin:
 * lock / unlock / activate / deactivate / reset-password.
 *
 * Tách riêng khỏi UserListController để không làm phức tạp flow hiện tại.
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Long id, RedirectAttributes ra) {
        return handleAction(id, ra, "lock");
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id, RedirectAttributes ra) {
        return handleAction(id, ra, "unlock");
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        return handleAction(id, ra, "activate");
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
        return handleAction(id, ra, "deactivate");
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes ra) {
        return handleAction(id, ra, "reset-password");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private String handleAction(Long id, RedirectAttributes ra, String action) {
        try {
            switch (action) {
                case "lock"           -> userAccountService.lockUser(id);
                case "unlock"         -> userAccountService.unlockUser(id);
                case "activate"       -> userAccountService.activateUser(id);
                case "deactivate"     -> userAccountService.deactivateUser(id);
                case "reset-password" -> userAccountService.adminResetPassword(id);
            }
            ra.addFlashAttribute("message", successMsg(action));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private String successMsg(String action) {
        return switch (action) {
            case "lock"           -> "Account has been locked";
            case "unlock"         -> "Account has been unlocked";
            case "activate"       -> "Account has been activated";
            case "deactivate"     -> "Account has been deactivated";
            case "reset-password" -> "Password reset — temporary password sent to user's email";
            default               -> "Action completed";
        };
    }
}
