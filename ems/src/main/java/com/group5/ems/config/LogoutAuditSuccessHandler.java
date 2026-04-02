package com.group5.ems.config;

import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.common.LogService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ghi audit LOGOUT vào {@link com.group5.ems.entity.AuditLog} trước khi redirect về trang login.
 */
@Component
@RequiredArgsConstructor
public class LogoutAuditSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final LogService       logService;
    private final UserRepository   userRepository;

    @PostConstruct
    void init() {
        setDefaultTargetUrl("/login?logout");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        if (authentication != null && authentication.getName() != null) {
            try {
                userRepository.findByUsername(authentication.getName())
                        .ifPresent(user -> logService.log(
                                AuditAction.LOGOUT,
                                AuditEntityType.AUTH,
                                user.getId(),
                                user.getId()));
            } catch (Exception e) {
                // Không chặn logout nếu audit DB lỗi
                System.err.println("[LogoutAudit] " + e.getMessage());
            }
        }
        super.onLogoutSuccess(request, response, authentication);
    }
}
