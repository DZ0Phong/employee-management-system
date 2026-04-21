package com.group5.ems.service;

import com.group5.ems.entity.Role;
import com.group5.ems.entity.User;
import com.group5.ems.entity.UserRole;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppUserDetailService implements UserDetailsService {

    private final UserRepository     userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Support login by email OR username
        User existingUser = userRepository.findByEmail(username)
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String status = existingUser.getStatus();
        if (status != null) {
            status = status.trim();
        }

        // ── LOCK5: brute-force tạm thời (30 phút) ──────────────────────────
        if ("LOCK5".equalsIgnoreCase(status)) {
            if (existingUser.getLockedUntil() != null
                    && existingUser.getLockedUntil().isBefore(LocalDateTime.now())) {
                // Hết 30 phút → tự động mở khoá
                existingUser.setStatus("ACTIVE");
                existingUser.setFailedLoginCount(0);
                existingUser.setLockedUntil(null);
                userRepository.save(existingUser);
                // tiếp tục load user bình thường bên dưới
            } else {
                long minutesLeft = java.time.Duration
                        .between(LocalDateTime.now(), existingUser.getLockedUntil())
                        .toMinutes() + 1;
                throw new LockedException("temp:" + minutesLeft);
            }
        }

        // ── LOCKED: admin khoá vô thời hạn ─────────────────────────────────
        if ("LOCKED".equalsIgnoreCase(status)) {
            throw new LockedException("admin");
        }

        // ── INACTIVE: chưa kích hoạt qua email ─────────────────────────────
        // Cho phép đăng nhập với đúng username/password,
        // sau đó redirect sang trang /activate ở CustomeLoginSuccessHandler.

        List<Role> userRoles = userRoleRepository.getRolesByUserId(existingUser.getId());
        List<GrantedAuthority> grantedAuthorities = userRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.builder()
                .username(existingUser.getUsername())
                .password(existingUser.getPasswordHash())
                .authorities(grantedAuthorities)
                .build();
    }
}
