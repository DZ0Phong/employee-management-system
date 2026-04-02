package com.group5.ems.service.auth.impl;

import com.group5.ems.entity.User;
import com.group5.ems.repository.UserRepository;
import com.group5.ems.service.auth.AccountActivationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AccountActivationServiceImpl implements AccountActivationService {

    private static final long OTP_TTL_SECONDS     = 600; // 10 phút
    private static final long RESEND_COOLDOWN_SEC  = 60;  // 1 phút

    private final UserRepository userRepository;
    private final JavaMailSender  mailSender;

    @Override
    @Transactional
    public boolean sendOtp(String email) {
        if (email == null || email.isBlank()) return false;

        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) return false;
        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) return false;

        // Cooldown: không gửi lại nếu vừa gửi trong 60 giây
        if (user.getActivationOtpExpiresAt() != null) {
            LocalDateTime sentAt = user.getActivationOtpExpiresAt().minusSeconds(OTP_TTL_SECONDS);
            if (sentAt.plusSeconds(RESEND_COOLDOWN_SEC).isAfter(LocalDateTime.now())) {
                return false; // trong cooldown
            }
        }

        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        user.setActivationOtp(otp);
        user.setActivationOtpExpiresAt(LocalDateTime.now().plusSeconds(OTP_TTL_SECONDS));
        userRepository.save(user);

        try {
            sendActivationEmail(user.getEmail(), user.getFullName(), otp);
        } catch (Exception e) {
            System.err.println("[Activation] Failed to send email: " + e.getMessage());
        }
        return true;
    }

    @Override
    @Transactional
    public VerifyResult verifyAndActivate(String email, String otp) {
        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            return VerifyResult.INVALID;
        }
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) return VerifyResult.NOT_FOUND;
        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) return VerifyResult.ALREADY_ACTIVE;
        if (user.getActivationOtp() == null) return VerifyResult.INVALID;
        if (!otp.trim().equals(user.getActivationOtp())) return VerifyResult.INVALID;
        if (user.getActivationOtpExpiresAt() == null
                || user.getActivationOtpExpiresAt().isBefore(LocalDateTime.now())) {
            return VerifyResult.EXPIRED;
        }

        user.setStatus("ACTIVE");
        user.setIsVerified(true);
        user.setActivationOtp(null);
        user.setActivationOtpExpiresAt(null);
        userRepository.save(user);
        return VerifyResult.OK;
    }

    @Override
    public long getRemainingSeconds(String email) {
        if (email == null || email.isBlank()) return 0;
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null || user.getActivationOtpExpiresAt() == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        if (user.getActivationOtpExpiresAt().isBefore(now)) return 0;
        return Duration.between(now, user.getActivationOtpExpiresAt()).getSeconds();
    }

    // ── email ─────────────────────────────────────────────────────────────────

    private void sendActivationEmail(String toEmail, String fullName, String otp)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        helper.setTo(toEmail);
        helper.setSubject("EMS Pro — Activate Your Account");

        String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/></head>" +
                "<body style=\"font-family:Arial,sans-serif;background:#f6f6f8;margin:0;padding:24px;\">" +
                "<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"max-width:560px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 16px rgba(0,0,0,.08);\">" +
                "<tr><td style=\"padding:28px 32px 12px;text-align:center;\">" +
                "<div style=\"width:48px;height:48px;background:#1414b8;border-radius:12px;display:inline-block;margin-bottom:12px;\"></div>" +
                "<h2 style=\"margin:0;font-size:18px;color:#0f172a;\">EMS Pro — Activate Your Account</h2>" +
                "</td></tr>" +
                "<tr><td style=\"padding:8px 32px;\">" +
                "<p style=\"font-size:14px;color:#374151;\">Hi <strong>" + (fullName != null ? fullName : "there") + "</strong>,</p>" +
                "<p style=\"font-size:14px;color:#374151;\">Your account has been created. Use the 6-digit code below to activate it and start using EMS Pro.</p>" +
                "</td></tr>" +
                "<tr><td style=\"padding:12px 32px;text-align:center;\">" +
                "<div style=\"display:inline-block;padding:14px 36px;background:#1414b8;color:#fff;font-size:26px;font-weight:700;letter-spacing:.2em;border-radius:9999px;\">" +
                otp +
                "</div>" +
                "</td></tr>" +
                "<tr><td style=\"padding:12px 32px 28px;\">" +
                "<p style=\"font-size:13px;color:#6b7280;\">This code is valid for <strong>10 minutes</strong>.</p>" +
                "<p style=\"font-size:12px;color:#9ca3af;\">If you did not request this, please ignore this email or contact your administrator.</p>" +
                "</td></tr>" +
                "</table></body></html>";

        helper.setText(html, true);
        mailSender.send(message);
    }
}
