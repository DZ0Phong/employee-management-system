package com.group5.ems.config;

import com.group5.ems.entity.User;
import com.group5.ems.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Nếu user đã đăng nhập nhưng status = INACTIVE thì không cho vào trang dashboard theo role nữa,
 * mà redirect sang /activate để tự gửi OTP và verify.
 */
@Component
@RequiredArgsConstructor
public class InactiveAccountDashboardRedirectFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    private static final Set<String> DASHBOARD_PATHS = Set.of(
            "/admin/dashboard",
            "/employee/dashboard",
            "/dept-manager/dashboard",
            "/hrmanager/dashboard",
            "/hr/dashboard"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Chỉ chặn GET dashboard (đảm bảo không ảnh hưởng POST/redirect khác)
        if (!"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ctx = request.getContextPath();
        String path = request.getRequestURI();
        if (StringUtils.hasLength(ctx) && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (!DASHBOARD_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !StringUtils.hasLength(auth.getName())) {
            filterChain.doFilter(request, response);
            return;
        }

        var userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String status = user.getStatus();
            if (status != null) {
                status = status.trim();
            }
            if ("INACTIVE".equalsIgnoreCase(status) && StringUtils.hasLength(user.getEmail())) {
                String email = UriUtils.encode(user.getEmail(), StandardCharsets.UTF_8);
                String target = ctx + "/activate?email=" + email;
                response.sendRedirect(target);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

