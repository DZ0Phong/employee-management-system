package com.group5.ems.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group5.ems.entity.User;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByStatus(String status);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByUsernameAndIdNot(String username, Long id);

    @Override
    @EntityGraph(attributePaths = {"employee", "employee.department", "userRoles", "userRoles.role"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "employee.department", "userRoles", "userRoles.role"})
    List<User> findTop5ByOrderByCreatedAtDesc();

    @Query("select count(u) from User u join u.employee e where u.status = :status")
    long countUsersWithEmployeeByStatus(@Param("status") String status);

    @Query("select count(u) from User u join u.employee e where u.status in :statuses")
    long countUsersWithEmployeeByStatuses(@Param("statuses") List<String> statuses);

    /**
     * Bulk-update: LOCK5 accounts whose lockedUntil has passed → ACTIVE.
     * Returns number of rows updated.
     */
    @Modifying
    @Query("UPDATE User u SET u.status = 'ACTIVE', u.failedLoginCount = 0, u.lockedUntil = NULL " +
           "WHERE u.status = 'LOCK5' AND u.lockedUntil IS NOT NULL AND u.lockedUntil <= :now")
    int unlockExpiredLock5(@Param("now") LocalDateTime now);

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.userRoles ur
            JOIN ur.role r
            WHERE r.code = :roleCode
              AND u.status = 'ACTIVE'
            """)
    List<User> findByRoleCode(@Param("roleCode") String roleCode);
}

