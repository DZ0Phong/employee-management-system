package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeInfoDTO {
    private Long id;
    private String fullName;
    private String firstName;
    private String avatarUrl;
    private String position;
    private String department;
}