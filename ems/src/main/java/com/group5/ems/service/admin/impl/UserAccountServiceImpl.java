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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.mail.username}")
    private String mailFrom;

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
    public String adminResetPassword(Long userId) {
        User user = findOrThrow(userId);
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
            return null; // email gửi thành công
        } catch (Exception e) {
            String err = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("[UserAccountService] Email failed for " + user.getEmail() + ": " + err);
            return "Password was reset but the email could not be sent — " + err;
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

        helper.setFrom(mailFrom);
        helper.setTo(toEmail);
        helper.setSubject("EMS Pro — Your Password Has Been Reset");

        String safePwd  = escapeHtml(tempPassword);
        String safeName = escapeHtml(fullName != null ? fullName : "there");

        String html =
            "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/></head>" +
            "<body style=\"font-family:Arial,sans-serif;background:#f6f6f8;margin:0;padding:24px;\">" +
            "<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"max-width:560px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 16px rgba(0,0,0,.08);\">" +
            // ── header ──────────────────────────────────────────────────────────
            "<tr><td style=\"padding:28px 32px 12px;text-align:center;\">" +
            "<div style=\"display:inline-block;padding:8px 20px;background:#1414b8;border-radius:10px;margin-bottom:12px;\">" +
            "<span style=\"color:#fff;font-size:16px;font-weight:900;letter-spacing:.05em;\">EMS Pro</span></div>" +
            "<h2 style=\"margin:8px 0 0;font-size:18px;color:#0f172a;\">Your password has been reset</h2>" +
            "</td></tr>" +
            // ── greeting ────────────────────────────────────────────────────────
            "<tr><td style=\"padding:16px 32px 8px;\">" +
            "<p style=\"font-size:14px;color:#374151;margin:0;\">Hi <strong>" + safeName + "</strong>,</p>" +
            "<p style=\"font-size:14px;color:#374151;margin:12px 0 0;\">An administrator has reset your password on <strong>EMS Pro</strong>. Use the credentials below to log in:</p>" +
            "</td></tr>" +
            // ── credentials card ────────────────────────────────────────────────
            "<tr><td style=\"padding:12px 32px;\">" +
            "<table width=\"100%\" style=\"background:#f8fafc;border-radius:10px;border:1px solid #e2e8f0;\">" +
            "<tr><td style=\"padding:14px 20px;border-bottom:1px solid #e2e8f0;\">" +
            "<span style=\"font-size:11px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:.08em;\">Login Email</span><br/>" +
            "<span style=\"font-size:14px;font-weight:600;color:#1e293b;\">" + escapeHtml(toEmail) + "</span>" +
            "</td></tr>" +
            "<tr><td style=\"padding:14px 20px;\">" +
            "<span style=\"font-size:11px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:.08em;\">Temporary Password</span><br/>" +
            "<span style=\"display:inline-block;margin-top:6px;padding:10px 24px;background:#1414b8;color:#fff;font-size:18px;font-weight:700;letter-spacing:.15em;border-radius:9999px;\">" +
            safePwd + "</span>" +
            "</td></tr></table>" +
            "</td></tr>" +
            // ── footer ──────────────────────────────────────────────────────────
            "<tr><td style=\"padding:12px 32px 28px;\">" +
            "<p style=\"font-size:13px;color:#6b7280;margin:0;\">Please log in and <strong>change your password immediately</strong>.</p>" +
            "<p style=\"font-size:12px;color:#9ca3af;margin:12px 0 0;\">If you did not request this reset, contact your system administrator immediately.</p>" +
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
