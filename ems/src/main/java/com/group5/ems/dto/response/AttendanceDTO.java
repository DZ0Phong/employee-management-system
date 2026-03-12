package com.group5.ems.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDTO {
    private Long id;
    private LocalDate workDate;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String status;
    private String note;

    // Computed: tính số giờ làm việc
    public String getDuration() {
        if (checkIn == null || checkOut == null) return "-";
        long minutes = ChronoUnit.MINUTES.between(checkIn, checkOut);
        if (minutes <= 0) return "-";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}