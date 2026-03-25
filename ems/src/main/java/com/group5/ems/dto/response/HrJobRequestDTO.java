package com.group5.ems.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HrJobRequestDTO {
    private Long   id;
    private String title;
    private String content;
    private String status;
    private String step;
    private boolean urgent;       
    private Long   employeeId;
    private Long   departmentId;  
    private String requestedByName;
    private String departmentName;
    private String submittedAtFormatted;
    private String updatedAtFormatted;
    private String rejectedReason;
}