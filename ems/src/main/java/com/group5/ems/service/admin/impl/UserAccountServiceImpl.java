package com.group5.ems.service.admin.impl;

import com.group5.ems.entity.User;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.admin.UserAccountService;
import com.group5.ems.service.common.LogService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private static final int MAX_TEMP_PASSWORD_LENGTH = 12;
    private static final String CHARS_UPPER  = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String CHARS_LOWER  = "abcdefghjkmnpqrstuvwxyz";
    private static final String CHARS_DIGITS = "23456789";
    private static final String CHARS_SPECIAL = "@#$%&*!";
    private static final String CHARS_ALL = CHARS_UPPER + CHARS_LOWER + CHARS_DIGITS + CHARS_SPECIAL;

    private final UserRepository   userRepository;
    private final PasswordEncoder  passwordEncoder;
    private final JavaMailSender   mailSender;
    private final LogService       logService;

    @Override
    @Transactional
    public void lockUser(Long userId) {
        User user = findOrThrow(userId);
        // Admin lock = LOCKED (vô thời hạn, không set lockedUntil)
        user.setStatus("LOCKED");
        user.setLockedUntil(null);
        userRepository.save(user);
        logService.log(AuditAction.LOCK, AuditEntityType.USER, userId);
    }

    @Override
    @Transactional
    public void unlockUser(Long userId) {
        User user = findOrThrow(userId);
        // Mở khoá cả 2 loại: LOCKED (admin) và LOCK5 (brute-force)
        user.setStatus("ACTIVE");
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        logService.log(AuditAction.UNLOCK, AuditEntityType.USER, userId);
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {
        User user = findOrThrow(userId);
        user.setStatus("ACTIVE");
        userRepository.save(user);
        logService.log(AuditAction.ACTIVATE, AuditEntityType.USER, userId);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        User user = findOrThrow(userId);
        user.setStatus("INACTIVE");
        userRepository.save(user);
        logService.log(AuditAction.DEACTIVATE, AuditEntityType.USER, userId);
    }

    @Override
    @Transactional
    public void adminResetPassword(Long userId) {
        User user = findOrThrow(userId);
        // Reset password cho admin thường phải cho phép user đăng nhập ngay với mật khẩu tạm.
        // Nếu user đang bị LOCK5/LOCKED mà không đổi status về ACTIVE thì có thể bị chặn hoặc lỗi runtime.
        String status = user.getStatus();
        if ("LOCK5".equalsIgnoreCase(status) || "LOCKED".equalsIgnoreCase(status)) {
            user.setStatus("ACTIVE");
        }
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        logService.log(AuditAction.RESET_PASSWORD, AuditEntityType.USER, userId);

        try {
            sendTempPasswordEmail(user.getEmail(), user.getFullName(), tempPassword);
        } catch (Exception e) {
            // email failure không roll back transaction — log và tiếp tục
            System.err.println("[UserAccountService] Failed to send reset-password email: " + e.getMessage());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private String generateTempPassword() {
        SecureRandom rng = new SecureRandom();
        char[] pwd = new char[MAX_TEMP_PASSWORD_LENGTH];
        // đảm bảo đủ loại ký tự
        pwd[0] = CHARS_UPPER .charAt(rng.nextInt(CHARS_UPPER .length()));
        pwd[1] = CHARS_LOWER .charAt(rng.nextInt(CHARS_LOWER .length()));
        pwd[2] = CHARS_DIGITS.charAt(rng.nextInt(CHARS_DIGITS.length()));
        pwd[3] = CHARS_SPECIAL.charAt(rng.nextInt(CHARS_SPECIAL.length()));
        for (int i = 4; i < MAX_TEMP_PASSWORD_LENGTH; i++) {
            pwd[i] = CHARS_ALL.charAt(rng.nextInt(CHARS_ALL.length()));
        }
        // shuffle
        for (int i = MAX_TEMP_PASSWORD_LENGTH - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = pwd[i]; pwd[i] = pwd[j]; pwd[j] = tmp;
        }
        return new String(pwd);
    }

    private void sendTempPasswordEmail(String toEmail, String fullName, String tempPassword)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        helper.setTo(toEmail);
        helper.setSubject("EMS Pro — Your Temporary Password");

        // Escape để tránh tình huống copy mật khẩu sai nếu password có ký tự HTML đặc biệt.
        String safePassword = escapeHtml(tempPassword);

        String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/></head>" +
                "<body style=\"font-family:Arial,sans-serif;background:#f6f6f8;margin:0;padding:24px;\">" +
                "<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"max-width:560px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 16px rgba(0,0,0,.08);\">" +
                "<tr><td style=\"padding:28px 32px 12px;text-align:center;\">" +
                "<div style=\"width:48px;height:48px;background:#1414b8;border-radius:12px;display:inline-flex;align-items:center;justify-content:center;margin-bottom:12px;\"></div>" +
                "<h2 style=\"margin:0;font-size:18px;color:#0f172a;\">EMS Pro — Temporary Password</h2>" +
                "</td></tr>" +
                "<tr><td style=\"padding:8px 32px;\">" +
                "<p style=\"font-size:14px;color:#374151;\">Hi <strong>" + fullName + "</strong>,</p>" +
                "<p style=\"font-size:14px;color:#374151;\">An administrator has reset your account password. Your temporary password is:</p>" +
                "</td></tr>" +
                "<tr><td style=\"padding:8px 32px;text-align:center;\">" +
                "<div style=\"display:inline-block;padding:14px 32px;background:#1414b8;color:#fff;font-size:20px;font-weight:700;letter-spacing:.15em;border-radius:9999px;\">" +
                safePassword + "</div>" +
                "</td></tr>" +
                "<tr><td style=\"padding:12px 32px 28px;\">" +
                "<p style=\"font-size:13px;color:#6b7280;\">Please log in and <strong>change your password immediately</strong> after receiving this email.</p>" +
                "<p style=\"font-size:12px;color:#9ca3af;\">If you did not request this, please contact your system administrator immediately.</p>" +
                "</td></tr>" +
                "</table></body></html>";

        helper.setText(html, true);
        mailSender.send(message);
    }

    private String escapeHtml(String input) {
        if (input == null) return null;
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
