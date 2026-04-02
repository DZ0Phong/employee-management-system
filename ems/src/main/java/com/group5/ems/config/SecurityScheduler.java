package com.group5.ems.config;

import com.group5.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduler tự động mở khoá các tài khoản LOCK5 (brute-force)
 * khi thời gian khoá 30 phút đã hết.
 *
 * Chạy mỗi 60 giây, cập nhật trực tiếp trong DB qua bulk update.
 */
@Component
@RequiredArgsConstructor
public class SecurityScheduler {

    private final UserRepository userRepository;

    /**
     * Mỗi 60 giây: tìm tất cả LOCK5 có lockedUntil <= NOW → reset về ACTIVE.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoUnlockExpiredBruteForceLocks() {
        int updated = userRepository.unlockExpiredLock5(LocalDateTime.now());
        if (updated > 0) {
            System.out.println("[SecurityScheduler] Auto-unlocked " + updated + " brute-force locked account(s).");
        }
    }
}
