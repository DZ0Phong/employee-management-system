package com.group5.ems.dto.response.hrmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyReviewDTO {
    private Long id;
    private String name;       // tên policy
    private String owner;      // người sở hữu
    private String status;     // IN_REVIEW, DRAFTING, FINALIZED
    private LocalDate deadline;
}