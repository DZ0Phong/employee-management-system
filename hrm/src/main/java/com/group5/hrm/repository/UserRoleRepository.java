package com.group5.hrm.repository;

import com.group5.hrm.entity.UserRole;
import com.group5.hrm.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}

