package com.group5.ems.config;

import com.group5.ems.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomeLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String ctx = request.getContextPath();
        var authorities = authentication.getAuthorities();

        // Chặn account INACTIVE ngay tại tầng success redirect (bảo hiểm thêm).
        String username = authentication != null ? authentication.getName() : null;
        if (username != null && !username.isBlank()) {
            userRepository.findByUsername(username.trim()).ifPresent(user -> {
                if ("INACTIVE".equalsIgnoreCase(user.getStatus())
                        && user.getEmail() != null && !user.getEmail().isBlank()) {
                    // Trim status để tránh trường hợp DB lưu kèm khoảng trắng.
                    try {
                        String email = UriUtils.encode(user.getEmail(), StandardCharsets.UTF_8);
                        response.sendRedirect(response.encodeRedirectURL(ctx + "/activate?email=" + email));
                    } catch (IOException ignored) {
                        // ignore
                    }
                }
            });
            if (response.isCommitted()) return;
        }

        if (hasRole(authorities, "ROLE_ADMIN")) {
            response.sendRedirect(response.encodeRedirectURL(ctx + "/admin/dashboard"));
            return;
        }
        if (hasRole(authorities, "ROLE_DEPT_MANAGER")) {
            response.sendRedirect(response.encodeRedirectURL(ctx + "/dept-manager/dashboard"));
            return;
        }
        if (hasRole(authorities, "ROLE_HR_MANAGER")) {
            response.sendRedirect(response.encodeRedirectURL(ctx + "/hrmanager/dashboard"));
            return;
        }
        if (hasRole(authorities, "ROLE_HR")) {
            response.sendRedirect(response.encodeRedirectURL(ctx + "/hr/dashboard"));
            return;
        }
        if (hasRole(authorities, "ROLE_EMPLOYEE")) {
            response.sendRedirect(response.encodeRedirectURL(ctx + "/employee/dashboard"));
            return;
        }
        response.sendRedirect(response.encodeRedirectURL(ctx + "/login?error"));
    }

    private boolean hasRole(Iterable<? extends GrantedAuthority> authorities, String role) {
        for (GrantedAuthority grantedAuthority : authorities) {
            if (role.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
