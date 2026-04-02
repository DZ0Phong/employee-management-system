package com.group5.ems.service.auth;

import com.group5.ems.entity.User;
import com.group5.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.reset-password-url:http://localhost:8080/forgot-password}")
    private String resetPasswordUrl;

    private static final long OTP_TTL_SECONDS = 600;      // 10 minutes
    private static final long RESEND_COOLDOWN_SECONDS = 120; // 2 minutes

    public enum OtpStatus {
        VALID,
        EXPIRED,
        INVALID
    }

    public enum PasswordStatus {
        OK,
        TOO_SHORT,
        NO_UPPERCASE,
        NO_NUMBER,
        NO_SPECIAL
    }

    public boolean sendPasswordResetEmail(String toEmail) throws MessagingException {
        if (toEmail == null || toEmail.isBlank()) {
            return false;
        }

        String trimmedEmail = toEmail.trim();

        if (!userRepository.existsByEmail(trimmedEmail)) {
            return false;
        }

        User user = userRepository.findByEmail(trimmedEmail).orElse(null);
        if (user == null) {
            return false;
        }

        // Generate 6-digit OTP and store with expiration
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        user.setResetOtp(otp);
        user.setResetOtpExpiresAt(LocalDateTime.now().plusSeconds(OTP_TTL_SECONDS));
        userRepository.save(user);

        String encodedEmail = UriUtils.encode(trimmedEmail, StandardCharsets.UTF_8);
        String link = resetPasswordUrl + "?email=" + encodedEmail;

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        helper.setTo(trimmedEmail);
        helper.setSubject("EMS Pro - Password Reset Request");

        String html =
                "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\" />" +
                "  <title>EMS Pro - Password Reset Request</title>" +
                "</head>" +
                "<body style=\"font-family: Arial, sans-serif; background-color:#f6f6f8; margin:0; padding:24px;\">" +
                "  <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;box-shadow:0 4px 16px rgba(15,23,42,0.12);\">" +
                "    <tr>" +
                "      <td style=\"padding:24px 32px 8px 32px; text-align:center;\">" +
                "        <h1 style=\"margin:0;font-size:22px;color:#0f172a;\">EMS Pro - Password Reset Request</h1>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style=\"padding:8px 32px 0 32px;\">" +
                "        <p style=\"font-size:14px;color:#4b5563;\">We received a request to reset the password for your EMS Pro account.</p>" +
                "        <p style=\"font-size:14px;color:#4b5563;\">Use the verification code below on the password reset page.</p>" +
                "        <p style=\"font-size:14px;color:#4b5563;\">You can also open the reset page directly using this link:</p>" +
                "        <p style=\"font-size:12px;\"><a href=\"" + link + "\" style=\"color:#1d4ed8;\">Open password reset page</a></p>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style=\"padding:16px 32px; text-align:center;\">" +
                "        <div style=\"display:inline-block;padding:12px 24px;border-radius:9999px;background:#1414b8;color:#ffffff;font-weight:700;font-size:20px;letter-spacing:0.3em;\">" +
                otp +
                "        </div>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style=\"padding:0 32px 24px 32px;\">" +
                "        <p style=\"font-size:12px;color:#6b7280;\">This code will expire in 10 minutes.</p>" +
                "        <p style=\"font-size:12px;color:#9ca3af;\">If you did not request a password reset, you can safely ignore this email.</p>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";

        helper.setText(html, true);

        mailSender.send(message);

        return true;
    }

    public long getRemainingOtpSeconds(String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null || user.getResetOtpExpiresAt() == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (user.getResetOtpExpiresAt().isBefore(now)) {
            return 0;
        }
        return java.time.Duration.between(now, user.getResetOtpExpiresAt()).getSeconds();
    }

    public boolean resendOtp(String email) throws MessagingException {
        if (email == null || email.isBlank()) {
            return false;
        }
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (user.getResetOtpExpiresAt() != null) {
            LocalDateTime lastSentAt = user.getResetOtpExpiresAt().minusSeconds(OTP_TTL_SECONDS);
            if (lastSentAt.isBefore(now) && lastSentAt.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
                // still in cooldown window
                return false;
            }
        }

        // Delegate to main send method which will generate new OTP and email
        return sendPasswordResetEmail(email);
    }

    public OtpStatus verifyResetOtp(String email, String otp) {
        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            return OtpStatus.INVALID;
        }

        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            return OtpStatus.INVALID;
        }

        if (user.getResetOtp() == null || user.getResetOtpExpiresAt() == null) {
            return OtpStatus.INVALID;
        }

        if (!otp.trim().equals(user.getResetOtp())) {
            return OtpStatus.INVALID;
        }

        if (user.getResetOtpExpiresAt().isBefore(LocalDateTime.now())) {
            return OtpStatus.EXPIRED;
        }

        // Thêm thời gian để nhập mật khẩu mới (timer step 2 có thể sát 0)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minValidUntil = now.plusMinutes(10);
        if (user.getResetOtpExpiresAt().isBefore(minValidUntil)) {
            user.setResetOtpExpiresAt(minValidUntil);
            userRepository.save(user);
        }

        return OtpStatus.VALID;
    }

    public PasswordStatus validatePassword(String rawPassword) {
        if (rawPassword == null) {
            return PasswordStatus.TOO_SHORT;
        }
        String pw = rawPassword.trim();

        if (pw.length() < 8) {
            return PasswordStatus.TOO_SHORT;
        }
        if (!pw.matches(".*[A-Z].*")) {
            return PasswordStatus.NO_UPPERCASE;
        }
        if (!pw.matches(".*\\d.*")) {
            return PasswordStatus.NO_NUMBER;
        }
        if (!pw.matches(".*[^A-Za-z0-9].*")) {
            return PasswordStatus.NO_SPECIAL;
        }
        return PasswordStatus.OK;
    }

    /**
     * Đặt lại mật khẩu sau forgot-password. Bắt buộc gửi lại đúng mã OTP đã verify ở bước trước
     * (trước đây chỉ kiểm tra hết hạn → dễ fail im lặng hoặc bỏ qua bước OTP).
     */
    public boolean resetPassword(String email, String newPassword, String otpCode) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if (otpCode == null || otpCode.isBlank()) {
            return false;
        }
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getResetOtp() == null || !otpCode.trim().equals(user.getResetOtp())) {
            return false;
        }
        if (user.getResetOtpExpiresAt() == null || user.getResetOtpExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        user.setResetOtp(null);
        user.setResetOtpExpiresAt(null);
        userRepository.save(user);
        return true;
    }
}

