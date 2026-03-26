package com.group5.ems.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomeLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        var authorities = authentication.getAuthorities();
        if (hasRole(authorities, "ROLE_ADMIN")) {
            response.sendRedirect("/admin/dashboard");
            return;
        }
        if (hasRole(authorities, "ROLE_DEPT_MANAGER")) {
            response.sendRedirect("/dept-manager/dashboard");
            return;
        }
        if (hasRole(authorities, "ROLE_HR_MANAGER")) {
            response.sendRedirect("/hrmanager/dashboard");
            return;
        }
        if (hasRole(authorities, "ROLE_HR")) {
            response.sendRedirect("/hr/dashboard");
            return;
        }
        if (hasRole(authorities, "ROLE_EMPLOYEE")) {
            response.sendRedirect("/employee/dashboard");
            return;
        }
        response.sendRedirect("/login?error");
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
