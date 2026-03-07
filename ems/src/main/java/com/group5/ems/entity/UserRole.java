package com.group5.ems.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
public class UserRole {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "granted_at", updatable = false)
    private LocalDateTime grantedAt;

    @Column(name = "granted_by")
    private Long grantedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

    @PrePersist
    protected void onCreate() {
        grantedAt = LocalDateTime.now();
    }

}

