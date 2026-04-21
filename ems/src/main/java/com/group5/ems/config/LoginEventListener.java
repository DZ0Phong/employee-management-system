package com.group5.ems.config;

import com.group5.ems.entity.User;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Lắng nghe Spring Security authentication events để:
 *  - Ghi lastLoginAt khi đăng nhập thành công
 *  - Đếm số lần đăng nhập thất bại, tự khoá tài khoản sau MAX_FAILURES lần
 *
 * Không sửa SecurityConfig hay CustomeLoginSuccessHandler.
 */
@Component
@RequiredArgsConstructor
public class LoginEventListener {

    private static final int    MAX_FAILURES     = 5;
    private static final int    LOCK_MINUTES     = 15;  // brute-force LOCK5: tự mở khoá sau 15 phút

    private final UserRepository userRepository;
    private final LogService     logService;

    @EventListener
    @Transactional
    public void onLoginSuccess(InteractiveAuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return;

        User user = opt.get();
        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        try {
            logService.log(AuditAction.LOGIN, AuditEntityType.AUTH, user.getId(), user.getId());
        } catch (Exception e) {
            System.err.println("[LoginEvent] LOGIN audit: " + e.getMessage());
        }
    }

    @EventListener
    @Transactional
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        String username = extractUsername(event.getAuthentication());
        if (username == null || username.isBlank()) {
            return;
        }
        // The form may submit an email address as "username", so try email first
        String t = username.trim();
        Optional<User> opt = userRepository.findByEmail(t).or(() -> userRepository.findByUsername(t));
        if (opt.isEmpty()) return;

        User user = opt.get();

        // Nếu đã bị khoá (LOCK5/LOCKED) hoặc INACTIVE: không increment counter.
        // LOCK5 = brute-force đang khoá, LOCKED = admin khoá, INACTIVE = chưa kích hoạt.
        String status = user.getStatus();
        if (status != null) {
            status = status.trim();
        }
        if ("LOCK5".equalsIgnoreCase(status)
                || "LOCKED".equalsIgnoreCase(status)
                || "INACTIVE".equalsIgnoreCase(status)) {
            try {
                logService.log(AuditAction.LOGIN_FAILED, AuditEntityType.AUTH, user.getId(), user.getId());
            } catch (Exception e) {
                System.err.println("[LoginEvent] LOGIN_FAILED audit: " + e.getMessage());
            }
            return;
        }

        int failCount = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(failCount);

        if (failCount >= MAX_FAILURES) {
            // LOCK5 = brute-force temporary lock (30 phút, tự mở)
            user.setStatus("LOCK5");
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }
        userRepository.save(user);

        try {
            logService.log(AuditAction.LOGIN_FAILED, AuditEntityType.AUTH, user.getId(), user.getId());
        } catch (Exception e) {
            System.err.println("[LoginEvent] LOGIN_FAILED audit: " + e.getMessage());
        }
    }

    private static String extractUsername(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object p = auth.getPrincipal();
        if (p instanceof String s) {
            return s;
        }
        if (p instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return null;
    }
}
