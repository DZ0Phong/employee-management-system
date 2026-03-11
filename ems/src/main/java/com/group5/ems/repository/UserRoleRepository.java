package com.group5.ems.repository;

import com.group5.ems.entity.Role;
import com.group5.ems.entity.User;
import com.group5.ems.entity.UserRole;
import com.group5.ems.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
    //lay role
    @Query("select r from UserRole ur join ur.role r where ur.userId = :userId ")
    List<Role> getRolesByUserId(@Param("userId") Long userId);

    @Query("select r from UserRole ur join ur.role r where ur.userId = :userId ")
    Role getRoleByUserId(@Param("userId") Long userId);


}

