package com.group5.ems.repository;

import com.group5.ems.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByStatus(String status);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByUsernameAndIdNot(String username, Long id);

    List<User> findTop5ByOrderByCreatedAtDesc();

    /**
     * Bulk-update: LOCK5 accounts whose lockedUntil has passed → ACTIVE.
     * Returns number of rows updated.
     */
    @Modifying
    @Query("UPDATE User u SET u.status = 'ACTIVE', u.failedLoginCount = 0, u.lockedUntil = NULL " +
           "WHERE u.status = 'LOCK5' AND u.lockedUntil IS NOT NULL AND u.lockedUntil <= :now")
    int unlockExpiredLock5(@Param("now") LocalDateTime now);
}

