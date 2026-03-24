package com.group5.ems.dto.response.hr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private Long employeeId;
    private String username;
    private String email;
    private String fullName;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private String status;
    private Boolean isVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // ── Các field không có trong DB, join từ bảng khác ──
    private String role;            // từ bảng roles
    private String departmentName;  // từ bảng departments

    // ── Computed, không map DB ───────────────────────────
    public String getLastLoginDisplay() {
        if (lastLoginAt == null) return "Never";
        long minutes = ChronoUnit.MINUTES.between(lastLoginAt, LocalDateTime.now());
        if (minutes < 1)  return "Just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24)   return hours + " hr ago";
        long days = hours / 24;
        if (days == 1)    return "Yesterday";
        return days + " days ago";
    }

    //get avt
    public String getInitials() {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}