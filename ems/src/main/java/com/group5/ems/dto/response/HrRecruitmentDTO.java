package com.group5.ems.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HrRecruitmentDTO {
    private Long   id;
    private String jobTitle;
    private String department;
    private Long departmentId;
    private String position;    
    private String status;
    private int    applicantCount;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryRange;
    private LocalDate openDate;
    private String    openDateFormatted;
    private LocalDate closeDate;
    private String    closeDateFormatted;
    private Long      daysUntilClose;
    private String    description;
    private String    requirements;
    private String    benefits;
}