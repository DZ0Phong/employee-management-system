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
        for( GrantedAuthority grantedAuthority : authorities ) {
            String role = grantedAuthority.getAuthority();

            switch (role) {
                case "ROLE_ADMIN" -> {
                    response.sendRedirect("/admin/dashboard");
                    return;
                }
                case "ROLE_EMPLOYEE" -> {
                    response.sendRedirect("/employee/dashboard");
                    return;
                }
                case "ROLE_DEPT_MANAGER" -> {
                    response.sendRedirect("/dept-manager/dashboard");
                    return;
                }
                case "ROLE_HR_MANAGER" -> {
                    response.sendRedirect("/hrmanager/dashboard");
                    return;
                }
                case "ROLE_HR" -> {
                    response.sendRedirect("/hr/dashboard");
                    return;
                }
            }

        }
        response.sendRedirect("/login?error");
    }
}
