package com.group5.ems.service.admin;

public interface UserAccountService {

    /** Khoá tài khoản thủ công (admin chủ động khoá) */
    void lockUser(Long userId);

    /** Mở khoá tài khoản (bao gồm reset brute-force counter) */
    void unlockUser(Long userId);

    /** Kích hoạt tài khoản (INACTIVE → ACTIVE) */
    void activateUser(Long userId);

    /** Vô hiệu hoá tài khoản (ACTIVE → INACTIVE) */
    void deactivateUser(Long userId);

    /**
     * Admin reset mật khẩu: sinh mật khẩu tạm, set hash, gửi email thông báo.
     */
    void adminResetPassword(Long userId);
}
