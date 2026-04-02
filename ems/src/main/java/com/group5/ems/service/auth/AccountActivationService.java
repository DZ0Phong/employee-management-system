package com.group5.ems.service.auth;

public interface AccountActivationService {

    /**
     * Gửi OTP kích hoạt tới email của user.
     * Trả về false nếu email không tồn tại hoặc tài khoản đã ACTIVE.
     */
    boolean sendOtp(String email);

    /** Kết quả xác minh OTP và kích hoạt tài khoản. */
    enum VerifyResult { OK, INVALID, EXPIRED, NOT_FOUND, ALREADY_ACTIVE }

    /**
     * Xác minh OTP → kích hoạt tài khoản (status = ACTIVE, isVerified = true).
     */
    VerifyResult verifyAndActivate(String email, String otp);

    /** Số giây còn lại trước khi OTP hiện tại hết hạn (0 = hết hoặc chưa gửi). */
    long getRemainingSeconds(String email);
}
