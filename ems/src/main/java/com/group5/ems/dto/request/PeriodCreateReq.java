package com.group5.ems.dto.request;

import com.group5.ems.dto.request.validation.ValidDateRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Request DTO for creating a new timesheet period.
 */
@ValidDateRange
@Builder
public record PeriodCreateReq(

        @NotBlank(message = "Period name is required")
        String periodName,

        @NotNull(message = "Start date is required")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate
) {
}
