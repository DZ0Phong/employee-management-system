package com.group5.hrm.repository;

import com.group5.hrm.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByEmailAndCodeAndTypeAndIsUsedFalseAndExpiredAtAfter(
            String email, String code, String type, LocalDateTime now);

    Optional<OtpVerification> findByUserIdAndCodeAndTypeAndIsUsedFalseAndExpiredAtAfter(
            Long userId, String code, String type, LocalDateTime now);

    List<OtpVerification> findByEmailAndTypeOrderByCreatedAtDesc(String email, String type, Pageable pageable);
}

