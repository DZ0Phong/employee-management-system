package com.group5.ems.config;

import com.group5.ems.entity.User;
import com.group5.ems.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.web.util.UriUtils;

/**
 * Phân loại lỗi đăng nhập và redirect đến URL có query param tương ứng.
 *
 * ?error          → sai mật khẩu / username không tồn tại
 * ?locked=temp    → brute-force (5 lần sai) — tự mở sau 5 phút
 * ?locked=admin   → admin khoá thủ công — vô thời hạn
 * ?disabled       → tài khoản INACTIVE
 */
@Component
@RequiredArgsConstructor
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String redirectUrl;

        if (exception instanceof LockedException) {
            // Account đã bị LOCKED trước khi attempt này (loadUserByUsername ném ra)
            redirectUrl = buildLockedUrl(exception.getMessage());

        } else if (exception instanceof DisabledException) {
            // INACTIVE → dẫn tới trang kích hoạt tài khoản qua email OTP
            String username = request.getParameter("username");
            redirectUrl = resolveDisabledRedirect(username);

        } else {
            // BadCredentialsException hoặc tương tự — kiểm tra xem
            // attempt này có vừa kích hoạt brute-force lock không
            String username = request.getParameter("username");
            redirectUrl = resolveAfterBadCredentials(username);
        }

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Sau khi sai mật khẩu, `LoginEventListener` đã cập nhật DB rồi.
     * Nếu user vừa bị khoá ngay attempt này → hiển thị thông báo đúng.
     */
    private Optional<User> findByEmailOrUsername(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        String t = input.trim();
        return userRepository.findByEmail(t).or(() -> userRepository.findByUsername(t));
    }

    private String resolveAfterBadCredentials(String username) {
        if (username == null || username.isBlank()) return "/login?error";

        Optional<User> opt = findByEmailOrUsername(username);
        if (opt.isEmpty()) return "/login?error";

        User user = opt.get();
        String status = user.getStatus();
        if (status != null) {
            status = status.trim();
        }

        // LOCK5: vừa bị brute-force lock ngay attempt này
        if ("LOCK5".equalsIgnoreCase(status) && user.getLockedUntil() != null) {
            long mins = Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            return "/login?locked=temp&minutes=" + mins;
        }
        // LOCKED: admin đã khoá (không nên vào đây qua bad-creds, nhưng phòng hờ)
        if ("LOCKED".equalsIgnoreCase(status)) {
            return "/login?locked=admin";
        }
        return "/login?error";
    }

    /** Tạo redirect URL từ message của LockedException (ném bởi AppUserDetailService). */
    private String buildLockedUrl(String msg) {
        if (msg != null && msg.startsWith("temp:")) {
            return "/login?locked=temp&minutes=" + msg.substring(5);
        }
        return "/login?locked=admin";
    }

    /**
     * INACTIVE account → dẫn tới trang kích hoạt, truyền email đã encode.
     * Các trường hợp khác (tài khoản bị disable vĩnh viễn, v.v.) → /login?disabled.
     */
    private String resolveDisabledRedirect(String username) {
        if (username == null || username.isBlank()) {
            return "/login?disabled";
        }
        return findByEmailOrUsername(username)
                .filter(u -> {
                    String st = u.getStatus();
                    if (st != null) {
                        st = st.trim();
                    }
                    return "INACTIVE".equalsIgnoreCase(st);
                })
                .map(u -> "/activate?email=" + UriUtils.encode(u.getEmail(), StandardCharsets.UTF_8))
                .orElse("/login?disabled");
    }
}
