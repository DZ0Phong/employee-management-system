package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityCategoryDTO {
    private String type;        // leave, payroll, status, hr
    private String label;       // Display name
    private String icon;        // Material icon name
    private Long count;         // Number of items
    private String color;       // Badge color class
    private String description; // Optional description
}
